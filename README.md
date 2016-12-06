# Description

Fake SSO is a module use to "host" vendor specific authentication's protocol.
It is design to isolate exotic implementation from standard ones.

Fake SSO already provided implementation for :

* [Ecole Directe](https://www.ecoledirecte.com) (from Statim / Charlemagne) SSO integration
* A simple cryptographic keyring system
* Pronote' webservices to display a "student's life tracking widget"

# How to

To add a new vendor specific SSO implementation you have to follow the 3 next steps

## 1. Write a controller that extends SSOController

		package fr.wseduc.sso.controllers;

		import fr.wseduc.webutils.http.BaseController;
		import org.vertx.java.core.json.JsonObject;

		public abstract class SSOController extends BaseController {

			public abstract void setSsoConfig(JsonObject ssoConfig);

		}

In `setSsoConfig`'s implementation you will recieve your specific configuration's properties.
You will have to typically proxy the call to remote's application authentication with an controller's action.
See example of `fr.wseduc.sso.controllers.CharlemagneController` to figure out that mechanics

## 2. Register your new SSO controller in deployment configuration's file

Add your configuration in `/deployment/fake-sso/conf.json.template` inside 'sso-controllers' array
It have to follow the next template :

		{
			"class" : "fr.wseduc.sso.controllers.YourSSOController",
			"config" : {
				// properties you need
			}
		}

## 3. Configure your external application's SSO in back-office UI

_note_ : Fake SSO URL path prefix is `sso`. So if you expose an HTTP action on `GET /yoursso/access`
then the complete route to configure in application's registry (back office) UI will be something like `sso/yoursso/access`

# About

* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Conseil Régional Nord Pas de Calais - Picardie

* Develop by : Open Digital Education

* Funded by : Région Nord Pas de Calais-Picardie


