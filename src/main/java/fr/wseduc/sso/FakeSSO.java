package fr.wseduc.sso;

import fr.wseduc.sso.controllers.keyring.CredentialsController;
import fr.wseduc.sso.controllers.keyring.KeyRingController;
import fr.wseduc.sso.controllers.SSOController;
import fr.wseduc.sso.services.keyring.impl.DefaultCredentialsService;
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.sso.services.keyring.impl.DefaultKeyRingService;
import org.entcore.common.http.BaseServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class FakeSSO extends BaseServer {

	@Override
	public void start() {
		super.start();

		KeyRingService keyRingService = new DefaultKeyRingService();
		DefaultCredentialsService credentialsService = new DefaultCredentialsService(
				container.config().getString("crypt-key", "_"));
		credentialsService.setKeyRingService(keyRingService);

		KeyRingController keyRingController = new KeyRingController();
		keyRingController.setKeyRingService(keyRingService);
		CredentialsController credentialsController = new CredentialsController();
		credentialsController.setKeyRingService(keyRingService);
		credentialsController.setCredentialsService(credentialsService);

		addController(keyRingController);
		addController(credentialsController);

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
