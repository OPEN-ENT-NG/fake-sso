# Description

Fake SSO module is used to manage vendor specific authentication's protocol and mimic SSO workflow for the end user
It is designed to isolate exotic implementations from standard ones.

Fake SSO already provided implementation for :

* [Ecole Directe](https://www.ecoledirecte.com) (from Statim / Charlemagne) SSO integration
* A simple cryptographic keyring system
* Pronote' webservices to display a "student's life tracking widget"
* [La Vie Scolaire.fr](http://www.axess-education.fr/la-vie-scolaire-fr/) RSA SSO integration

# How to

To add a new vendor specific SSO implementation, follow the 3 next steps

## 1. Write a controller that extends SSOController

		package fr.wseduc.sso.controllers;

		import fr.wseduc.webutils.http.BaseController;
		import org.vertx.java.core.json.JsonObject;

		public abstract class SSOController extends BaseController {

			public abstract void setSsoConfig(JsonObject ssoConfig);

		}

In `setSsoConfig`'s implementation you will recieve your specific configuration's properties.
Typically you proxy the remote service authentication in a controller endpoint.
See example of `fr.wseduc.sso.controllers.CharlemagneController` to figure out that mechanics

## 2. Declare your new SSO controller in deployment configuration's file

Complete 'sso-controllers array 'in `/deployment/fake-sso/conf.json.template`. Your entry must follow the next schema :

		{
			"class" : "fr.wseduc.sso.controllers.YourSSOController",
			"config" : {
				// properties you need
			}
		}

## 3. Configure your external application's SSO in back-office UI

_note_ : Fake SSO URL path prefix is `sso`. So if you expose an HTTP action on `GET /yoursso/access`
then the complete route to configure in application's registry (back office) UI will be something like `sso/yoursso/access`

## 4. Configure external application's SSO : la-vie-scolaire.fr

* Update "sso-controllers" property object in ent-core.json file :


           "class" : "fr.wseduc.sso.controllers.VieScolaireFRController",
           "config" : {
              "appli" : "IBRD-REC-QK",
              "connection-timeout" : 2000,
              "URL_PROPERTY_END_LVS" : "/vsn.main/autoLoginTicketSession/getTicket/",
              "public-key" :"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7OmJIKfnHAauWnSwEAhi6s+8cIO0Y5HDhe7Oqm9Ko5wRWzUkEW1oDeGmaz/3L8gtZ4JprRvJOCkJETN+/Aq+S1vWMdxyjeLAXV7fI9Dlrb2MHccbmmhSC9Fic72R+AcR9Bo3oVzU3XpigP7DnZ5gI36Cmo2RTYrg7w4cj7XZm5mbW5lqr2DZNAnurWV81yWLiia88T53Kk72GMQg0OrIBMkh3cx+6pUeTRcdGNzUAhXI5xmiqB8vRdL9AKv+eFntKai3RD9LLKp6+njLzayscXIsoN6XHjFFhBg8v4xOlxp+fd+BWw4pMh0l4bjdaEIKAc4WyTHsRu7OjJeWj6m0mwIDAQAB"
           }
 In this block, set the application code in property "appli" defining your platform name.
 
* When adding a connector "la-vie-scolaire.fr" in the administration panel, set these properties :
   
  * Identifiant : Example : "Laviescolaire.fr" : connector's id
  * Nom d'affichage : Example : "La Vie Scolaire" : connector's displayed name
  * **URL** : "**/sso/viescolairefr?url=https%3A%2F%2F**$URL_TO_LA_VIESCOLAIRE$" 
    * Replace $URL_TO_LA_VIESCOLAIRE$ with the url to the "la-vie-scolaire.fr" server.
    * Example of URL : /sso/viescolairefr?url=https%3A%2F%2Fetablissementsso.la-vie-scolaire.fr
    * This URL is given by the "la-vie-scolaire.fr" editor.
    

# About

* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Conseil Régional Nord Pas de Calais - Picardie

* Develop by : Open Digital Education

* Funded by : Région Nord Pas de Calais-Picardie


