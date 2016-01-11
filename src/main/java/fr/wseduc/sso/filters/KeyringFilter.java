package fr.wseduc.sso.filters;


import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class KeyringFilter implements ResourcesProvider {

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
		final String accessService = "/keyring/access/" + resourceRequest.params().get("serviceId");
		if (user.getApps() != null) {
			for (UserInfos.Application app : user.getApps()) {
				if (app.getAddress() != null && app.getAddress().contains(accessService)) {
					handler.handle(true);
					return;
				}
			}
		}
		handler.handle(false);
	}

}
