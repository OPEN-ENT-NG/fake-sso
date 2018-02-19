/*
 * Copyright © Région Nord Pas de Calais-Picardie, Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.wseduc.sso.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.security.Md5;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

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
