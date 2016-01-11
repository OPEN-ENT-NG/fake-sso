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
