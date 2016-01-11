package fr.wseduc.sso.controllers.keyring;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class KeyRingController extends BaseController {

	private KeyRingService keyRingService;

	@Get("/keyring/admin-console")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void adminView(HttpServerRequest request) {
		renderView(request);
	}

	@Post("/keyring")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addService(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "createService", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject data) {
				keyRingService.create(data, notEmptyResponseHandler(request, 201));
			}
		});
	}

	@Put("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void alterService(final HttpServerRequest request) {
		// TODO add validation
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject data) {
				keyRingService.alter(request.params().get("serviceId"), data, notEmptyResponseHandler(request));
			}
		});
	}

	@Get("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void descService(HttpServerRequest request) {
		keyRingService.description(request.params().get("serviceId"), notEmptyResponseHandler(request));
	}

	@Get("/keyring")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listServices(HttpServerRequest request) {
		keyRingService.listServices(arrayResponseHandler(request));
	}

	@Delete("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void dropService(HttpServerRequest request) {
		keyRingService.drop(request.params().get("serviceId"), defaultResponseHandler(request, 204));
	}

	public void setKeyRingService(KeyRingService keyRingService) {
		this.keyRingService = keyRingService;
	}

}
