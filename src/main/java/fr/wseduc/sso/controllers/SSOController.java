package fr.wseduc.sso.controllers;

import fr.wseduc.webutils.http.BaseController;
import org.vertx.java.core.json.JsonObject;

public abstract class SSOController extends BaseController {

	public abstract void setSsoConfig(JsonObject ssoConfig);

}
