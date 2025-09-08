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

package fr.wseduc.sso;

import fr.wseduc.sso.controllers.keyring.CredentialsController;
import fr.wseduc.sso.controllers.keyring.KeyRingController;
import fr.wseduc.sso.controllers.SSOController;
import fr.wseduc.sso.services.keyring.impl.DefaultCredentialsService;
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.sso.services.keyring.impl.DefaultKeyRingService;
import io.vertx.core.Future;
import org.entcore.common.http.BaseServer;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FakeSSO extends BaseServer {

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
        final Promise<Void> promise = Promise.promise();
        super.start(promise);
        promise.future()
            .compose(e -> init())
            .onComplete(startPromise);
    }

    private Future<Void> init() {
        Future<Void> future;
        try {
            KeyRingService keyRingService = new DefaultKeyRingService();
            DefaultCredentialsService credentialsService = new DefaultCredentialsService(
                config.getString("crypt-key", "_"));
            credentialsService.setKeyRingService(keyRingService);

            KeyRingController keyRingController = new KeyRingController();
            keyRingController.setKeyRingService(keyRingService);
            CredentialsController credentialsController = new CredentialsController();
            credentialsController.setKeyRingService(keyRingService);
            credentialsController.setCredentialsService(credentialsService);

            addController(keyRingController);
            addController(credentialsController);

            loadSSOControllers();
            future = Future.succeededFuture();
        } catch (Exception e) {
            future = Future.failedFuture(e);
        }
        return future;
	}

	private void loadSSOControllers() {
		JsonArray sc = config.getJsonArray("sso-controllers");
		if (sc != null) {
			for (Object o : sc) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject j = (JsonObject) o;
				String className = j.getString("class");
				if (className != null && !className.trim().isEmpty()) {
					try {
						SSOController controller = (SSOController) Class.forName(className).newInstance();
						controller.setSsoConfig(j.getJsonObject("config"));
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
