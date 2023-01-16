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
import fr.wseduc.webutils.security.AES128CBC;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.security.InvalidKeyException;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class Additio extends SSOController
{
    private static class AdditioConfig
    {
        private String url = "https://web.additioapp.com/access/partners";
        private String partnerParam = "p";
        private String partnerId;
        private String hashParam = "appid";
        private String secretKey;
        private String temporalFormat = "yyMMdd";
        private DateTimeFormatter formatter;
        private boolean temporalBeforeKey = true;

        public AdditioConfig(JsonObject ssoConfig)
        {
            this.url = ssoConfig.getString("url", this.url);
            this.partnerParam = ssoConfig.getString("partner-param", this.partnerParam);
            this.hashParam = ssoConfig.getString("hash-param", this.hashParam);
            this.partnerId = ssoConfig.getString("partner-id");
            this.secretKey = ssoConfig.getString("secret-key");
            this.temporalFormat = ssoConfig.getString("temporal-format", this.temporalFormat);
            this.temporalBeforeKey = ssoConfig.getBoolean("temporal-before-key", this.temporalBeforeKey);

            this.formatter = DateTimeFormatter.ofPattern(this.temporalFormat);
        }

        public String getKey()
        {
            String temporal = this.formatter.format(ZonedDateTime.ofInstant(Instant.now(), ZoneId.from(ZoneOffset.UTC)));
            return this.temporalBeforeKey ? temporal + this.secretKey : this.secretKey + temporal;
        }

        public String buildUrl(String hash)
        {
            try
            {
                return this.url + "?" + this.partnerParam + "=" + this.partnerId + "&" + this.hashParam + "=" + URLEncoder.encode(hash, "UTF-8");
            }
            catch(UnsupportedEncodingException e) { /* Cannot happen */ throw new RuntimeException(e); }
        }

        public boolean isValid()
        {
            return this.isValid(this.partnerId) && this.isValid(this.secretKey);
        }

        private boolean isValid(String s)
        {
            return s != null && s.length() > 0;
        }
    }

    private AdditioConfig config;

	@Get("/additio")
	@SecuredAction(value = "additio", type = ActionType.AUTHENTICATED)
	public void access(final HttpServerRequest request)
    {
		if (this.config == null || this.config.isValid() == false)
        {
			log.error("Invalid Additio configuration");
			renderError(request);
			return;
		}

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>()
        {
			@Override
			public void handle(UserInfos user)
            {
				if (user != null)
                {
                    try
                    {
                        JsonObject hashContent = new JsonObject().put("user_guid", user.getExternalId());

                        String url = config.buildUrl(AES128CBC.encrypt(hashContent.toString(), config.getKey()));

                        request.response().setStatusCode(302);
                        request.response().putHeader("Location", url);
                        request.response().end();
                    }
                    catch(InvalidKeyException e)
                    {
                        renderError(request, new JsonObject().put("error", e.getMessage()));
                    }
				}
                else
					unauthorized(request);
			}
		});

	}

	@Override
	public void setSsoConfig(JsonObject ssoConfig)
    {
        this.config = new AdditioConfig(ssoConfig);
	}

}
