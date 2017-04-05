package fr.wseduc.sso.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.sso.services.pronote.PronoteService;
import fr.wseduc.sso.services.pronote.PronoteServiceImpl;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Pronote extends SSOController {

	private PronoteService pronoteService;
	private String casCollection;
	private String pronoteContext;
	private static final String PROXY_METHOD = "proxyValidate";
	private long responseTimeout;
	private static final Logger log = LoggerFactory.getLogger(Pronote.class);

	public Pronote() {
		pronoteService = new PronoteServiceImpl();
	}

	@Get("/pronote")
	@SecuredAction(value = "pronote", type = ActionType.AUTHENTICATED)
	public void getData(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String userId = user.getUserId();
					pronoteService.getFromCache(userId, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if (event != null && event.getString("user") != null) {
								//from cache
								renderJson(request, event.getArray("results"));
							} else {
								pronoteService.getPronoteApps(user.getGroupsIds(), new Handler<Either<String, JsonArray>>() {
									@Override
									public void handle(Either<String, JsonArray> event) {
										if (event.isRight()) {
											final JsonArray appArray = event.right().getValue();
											if (appArray.size() > 0) {
												pronoteService.generateTicketByApp(appArray, userId, pronoteContext, casCollection, new Handler<Either<String, JsonArray>>() {
													@Override
													public void handle(Either<String, JsonArray> event) {
														if (event.isRight()) {
															final JsonArray ja = event.right().getValue();

															final AtomicInteger callCount = new AtomicInteger(ja.size());
															final List<String> errors = new ArrayList<String>();

															for (int i = 0; i < ja.size(); i++) {
																final JsonObject joResult = ja.get(i);
																final JsonArray jaResult = new JsonArray();
																callPronote(joResult, new Handler<JsonObject>() {
																	@Override
																	public void handle(JsonObject event) {
																		if (event.getString("status").equals("ok")) {
																			joResult.removeField("ticket");
																			joResult.putString("xmlResponse", event.getString("xml"));
																			jaResult.addObject(joResult);
																		} else {
																			//Tolerance to the failure
																			log.debug("Fail to call pronote : Url --> " + joResult.getString("address", "") + ", message : " +  event.getString("message"));
																			errors.add(event.getString("message", "pronote.call.error"));
																		}

																		if (callCount.decrementAndGet() == 0) {
																			//If there is at least one result
																			if (jaResult.size() > 0) {
																				pronoteService.storeInCache(userId, jaResult, new Handler<Boolean>() {
																					@Override
																					public void handle(Boolean event) {
																						if (event) {
																							renderJson(request, jaResult);
																						} else {
																							renderError(request);
																						}
																					}
																				});
																			} else {
																				//Only the first error is sent
																				renderError(request, new JsonObject().putString("error", errors.get(0)));
																			}
																		}
																	}
																});
															}
														} else {
															Renders.renderError(request, new JsonObject().putString("error", event.left().getValue()));
														}
													}
												});
											} else {
												Renders.renderError(request, new JsonObject().putString("error", "pronote.unregistered.error"));
											}
										} else {
											Renders.renderError(request);
										}
									}
								});
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void callPronote(JsonObject jo, final Handler<JsonObject> handler) {
		//two successive errors can be received (connection timeout + timeout period exceeded)
		final AtomicBoolean responseIsSent = new AtomicBoolean(false);
		URI pronoteUri = null;
		try {
			final String service = jo.getString("address", "");
			final String urlSeparator = service.endsWith("/")  ? "" : "/";
			pronoteUri = new URI(service + urlSeparator + pronoteContext);
		} catch (URISyntaxException e) {
			log.debug("Invalid pronote web service uri", e);
			handler.handle(new JsonObject().putString("status", "error").putString("message", "pronote.uri.error"));
		}

		if (pronoteUri != null) {
			final HttpClient httpClient = generateHttpClient(pronoteUri);

			final HttpClientRequest httpClientRequest = httpClient.post(pronoteUri.toString()  + "?ticket=" + jo.getString("ticket") + "&methode=" + PROXY_METHOD, new Handler<HttpClientResponse>() {
				@Override
				public void handle(HttpClientResponse response) {
					if (response.statusCode() == 200) {
						final Buffer buff = new Buffer();
						response.dataHandler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								buff.appendBuffer(event);
							}
						});
						response.endHandler(new Handler<Void>() {
							@Override
							public void handle(Void end) {
								final String xml = buff.toString();
								handler.handle(new JsonObject().putString("status", "ok").putString("xml", xml));
							}
						});
					} else {
						log.debug(response.statusMessage());
						response.bodyHandler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								log.debug("Returning body after PT CALL : " + event.toString("UTF-8"));
							}
						});
						handler.handle(new JsonObject().putString("status", "error").putString("message", "pronote.access.error"));
					}
					if (!responseIsSent.getAndSet(true)) {
						httpClient.close();
					}
				}
			});

			httpClientRequest.headers().set("Content-Length", "0");
			httpClientRequest.setTimeout(responseTimeout);
			//Typically an unresolved Address, a timeout about connection or response
			httpClientRequest.exceptionHandler(new Handler<Throwable>() {
				@Override
				public void handle(Throwable event) {
					log.debug(event.getMessage(), event);
					if (!responseIsSent.getAndSet(true)) {
						handler.handle(new JsonObject().putString("status", "error").putString("message", "pronote.connection.error"));
						httpClient.close();
					}
				}
			}).end();
		}

	}

	private HttpClient generateHttpClient(URI uri) {
		return vertx.createHttpClient()
				.setHost(uri.getHost())
				.setPort((uri.getPort() > 0) ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80))
				.setVerifyHost(false)
				.setTrustAll(true)
				.setSSL("https".equals(uri.getScheme()))
				.setKeepAlive(false);
	}

	@Override
	public void setSsoConfig(JsonObject ssoConfig) {
		casCollection = ssoConfig.getString("cas-collection", "authcas");
		responseTimeout = ssoConfig.getLong("response-timeout", 2000);
		pronoteContext = ssoConfig.getString("pronote-context", "donneesUtilisateur");
	}

}
