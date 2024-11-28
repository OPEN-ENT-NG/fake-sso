package fr.wseduc.sso.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.sso.services.pronote.PronoteService;
import fr.wseduc.sso.services.pronote.PronoteServiceImpl;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.*;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.MalformedURLException;
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
								renderJson(request, event.getJsonArray("results"));
							} else {
								pronoteService.getPronoteApps(user.getGroupsIds(), new Handler<Either<String, JsonArray>>() {
									@Override
									public void handle(Either<String, JsonArray> event) {
										if (event.isRight()) {
											final JsonArray appArray = event.right().getValue();
											if (appArray.size() > 0) {
												pronoteService.generateTicketByApp(appArray, user, pronoteContext, casCollection, new Handler<Either<String, JsonArray>>() {
													@Override
													public void handle(Either<String, JsonArray> event) {
														if (event.isRight()) {
															final JsonArray ja = event.right().getValue();

															final AtomicInteger callCount = new AtomicInteger(ja.size());
															final List<String> errors = new ArrayList<String>();

															for (int i = 0; i < ja.size(); i++) {
																final JsonObject joResult = ja.getJsonObject(i);
																final JsonArray jaResult = new JsonArray();
																callPronote(joResult, new Handler<JsonObject>() {
																	@Override
																	public void handle(JsonObject event) {
																		if (event.getString("status").equals("ok")) {
																			joResult.remove("ticket");
																			joResult.put("xmlResponse", event.getString("xml"));
																			jaResult.add(joResult);
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
																				renderError(request, new JsonObject().put("error", errors.get(0)));
																			}
																		}
																	}
																});
															}
														} else {
															Renders.renderError(request, new JsonObject().put("error", event.left().getValue()));
														}
													}
												});
											} else {
												Renders.renderError(request, new JsonObject().put("error", "pronote.unregistered.error"));
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
		final String service = jo.getString("address", "");
		URI pronoteUri = null;
		try {
			final String urlSeparator = service.endsWith("/")  ? "" : "/";
			pronoteUri = new URI(service + urlSeparator + pronoteContext);
		} catch (URISyntaxException e) {
			log.error("Invalid pronote web service uri for : " + service, e);
			handler.handle(new JsonObject().put("status", "error").put("message", "pronote.uri.error"));
		}

		///!\ an uri can not be null with a host null due to a char "_" in the FQDN
		// uri.getHost() is therefore replaced by uri.toURL().getHost()
		if (pronoteUri != null) {
			String host = null;
			try {
				host = pronoteUri.toURL().getHost();
			} catch (MalformedURLException e) {
				log.error("Invalid pronote web service uri for : " + service, e);
				handler.handle(new JsonObject().put("status", "error").put("message", "pronote.uri.error"));
			}

			if (host != null) {
				final int port = pronoteUri.getPort();
				final String scheme= pronoteUri.getScheme();

				final HttpClient httpClient = generateHttpClient(host, port, scheme);
				final String pronoteUrl = pronoteUri.toString() + "?ticket=" + jo.getString("ticket") + "&methode=" + PROXY_METHOD;
				final RequestOptions options = new RequestOptions()
					.setMethod(HttpMethod.POST)
					.setURI(pronoteUrl)
					.addHeader("Content-Length",  "0")
					.setTimeout(responseTimeout);
				httpClient.request(options)
				.flatMap(r -> r.send())
				.onSuccess(response -> {
					if (response.statusCode() == 200) {
						final Buffer buff = Buffer.buffer();
						response.handler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								buff.appendBuffer(event);
							}
						});
						response.endHandler(new Handler<Void>() {
							@Override
							public void handle(Void end) {
								final String xml = buff.toString();
								handler.handle(new JsonObject().put("status", "ok").put("xml", xml));
								if (!responseIsSent.getAndSet(true)) {
									httpClient.close();
								}
							}
						});
					} else {
						log.debug(response.statusMessage());
						response.bodyHandler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								log.debug("Returning body after PT CALL : " + pronoteUrl + ", Returning body : " + event.toString("UTF-8"));
								if (!responseIsSent.getAndSet(true)) {
									httpClient.close();
								}
							}
						});
						handler.handle(new JsonObject().put("status", "error").put("message", "pronote.access.error"));
					}
				}).onFailure(event -> {
					log.debug(event.getMessage(), event);
					if (!responseIsSent.getAndSet(true)) {
						handler.handle(new JsonObject().put("status", "error").put("message", "pronote.connection.error"));
						httpClient.close();
					}
				});
			}
		}
	}

	private HttpClient generateHttpClient(final String host, final int port, final String scheme) {
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(host)
				.setDefaultPort((port > 0) ? port : ("https".equals(scheme) ? 443 : 80))
				.setVerifyHost(false)
				.setTrustAll(true)
				.setSsl("https".equals(scheme))
				.setKeepAlive(false);
		return vertx.createHttpClient(options);
	}

	@Override
	public void setSsoConfig(JsonObject ssoConfig) {
		casCollection = ssoConfig.getString("cas-collection", "authcas");
		responseTimeout = ssoConfig.getLong("response-timeout", 2000l);
		pronoteContext = ssoConfig.getString("pronote-context", "donneesUtilisateur");
	}

}
