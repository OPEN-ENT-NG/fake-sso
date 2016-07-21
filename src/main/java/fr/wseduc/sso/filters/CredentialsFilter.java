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

package fr.wseduc.sso.filters;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class CredentialsFilter implements ResourcesProvider {

	@Override
	public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user, final Handler<Boolean> handler) {
		resourceRequest.pause();
		String query = "SELECT count(*) as count FROM sso." + resourceRequest.params().get("serviceId") +
				" WHERE id = ? AND user_id = ?";
		Sql.getInstance().prepared(query, new JsonArray().add(resourceRequest.params().get("id")).add(user.getUserId()),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				resourceRequest.resume();
				handler.handle("ok".equals(message.body().getString("status")) &&
						message.body().getArray("results") != null && message.body().getArray("results").size() == 1 &&
						message.body().getArray("results").get(0) != null &&
						message.body().getArray("results").<JsonArray>get(0).size() == 1 &&
						1l == message.body().getArray("results").<JsonArray>get(0).<Long>get(0)
				);
			}
		});
	}

}
