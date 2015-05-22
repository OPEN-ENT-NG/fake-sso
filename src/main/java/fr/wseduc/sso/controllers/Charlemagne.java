package fr.wseduc.sso.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.security.Md5;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Charlemagne extends SSOController {

	private String url;
	private String teacherUrl;
	private String siteId;
	private String privateKey;
	private boolean configOk = false;
	private static final String DATE_FORMAT = "ddMMyyyyHHmm";

	@Get("/charlemagne")
	@SecuredAction(value = "charlemagne", type = ActionType.AUTHENTICATED)
	public void access(final HttpServerRequest request) {
		if (!configOk) {
			log.error("Invalid Charlemagne configuration");
			renderError(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String host = ("Teacher".equals(user.getType())) ? teacherUrl : url;
					StringBuilder sb = new StringBuilder("?idunique=")
							.append(siteId).append(";").append(user.getExternalId())
							.append("&key=");
					try {
						sb.append(Md5.hash(privateKey + user.getExternalId() +
								new SimpleDateFormat(DATE_FORMAT).format(new Date())));
					} catch (NoSuchAlgorithmException e) {
						log.error(e.getMessage(), e);
						renderError(request);
						return;
					}
					redirect(request, host, sb.toString());
				} else {
					unauthorized(request);
				}
			}
		});

	}

	@Override
	public void setSsoConfig(JsonObject ssoConfig) {
		if (ssoConfig == null || ssoConfig.getString("url", "").isEmpty() ||
				ssoConfig.getString("teacherUrl", "").isEmpty() || ssoConfig.getString("siteId", "").isEmpty() ||
				ssoConfig.getString("privateKey", "").isEmpty()) {
			log.error("Invalid Charlemagne configuration");
		} else {
			url = ssoConfig.getString("url");
			teacherUrl = ssoConfig.getString("teacherUrl");
			siteId = ssoConfig.getString("siteId");
			privateKey = ssoConfig.getString("privateKey");
			configOk = true;
		}
	}

}
