package fr.wseduc.sso;

import fr.wseduc.sso.controllers.SSOController;
import org.entcore.common.http.BaseServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class FakeSSO extends BaseServer {

	@Override
	public void start() {
		super.start();

		loadSSOControllers();
	}

	private void loadSSOControllers() {
		JsonArray sc = config.getArray("sso-controllers");
		if (sc != null) {
			for (Object o : sc) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject j = (JsonObject) o;
				String className = j.getString("class");
				if (className != null && !className.trim().isEmpty()) {
					try {
						SSOController controller = (SSOController) Class.forName(className).newInstance();
						controller.setSsoConfig(j.getObject("config"));
						addController(controller);
						log.info("Init SSO controller : " + className);
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
						log.error("Instantiation error for class " + className, e);
					}
				}
			}
		}
	}

}
