package fr.wseduc.sso.services.pronote;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class PronoteServiceImpl implements PronoteService {

	private final Neo4j neo4j = Neo4j.getInstance();
	private MongoDb mongo = MongoDb.getInstance();
	private static final Logger log = LoggerFactory.getLogger(PronoteServiceImpl.class);
	private static final String PRONOTE_CAS_TYPE = "PronoteRegisteredService";
	private static final String FAKE_PGT_URL = "https://fakeSSO/sso/pronote";
	private static final String CACHE_COLLECTION = "pronotecache";

	public PronoteServiceImpl() {

	}

	@Override
	public void getPronoteApps(List<String> groupIds,
							   final Handler<Either<String, JsonArray>> handler) {
		String query =
				"MATCH (u:User)-[:IN]->(g:Group)-[:AUTHORIZED]-(role:Role)-[:AUTHORIZE]-(a:Action)-[:PROVIDE]-(app:Application) " +
						"WHERE app.casType = {pronoteCasType} AND g.id IN {groupIds} return distinct app.structureId, app.address";

		JsonObject params = new JsonObject()
				.putString("pronoteCasType", PRONOTE_CAS_TYPE)
				.putArray("groupIds", new JsonArray(groupIds.toArray()));

		neo4j.execute(query, params, validResultHandler(handler));
	}

	public void generateTicketByApp(JsonArray appArray, String userId, String pronoteContext, String collection, final Handler<Either<String, JsonArray>> handler) {

		final JsonArray jaResult = new JsonArray();
		final JsonArray jaCAS = new JsonArray();

		for (int i = 0; i < appArray.size(); i++) {
			final JsonObject joApp = appArray.get(i);
			final String ticket = generateCasJson(userId, pronoteContext, jaCAS, joApp);

			final JsonObject joResult = new JsonObject();
			joResult.putString("ticket", ticket);
			joResult.putString("structureId", joApp.getString("app.structureId"));
			joResult.putString("address", joApp.getString("app.address"));

			jaResult.addObject(joResult);
		}

		mongo.insert(collection, jaCAS, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					handler.handle(new Either.Right<String, JsonArray>(jaResult));

				} else {
					handler.handle(new Either.Left<String, JsonArray>(event.body().getString("message")));
				}
			}
		});
	}

	@Override
	public void getFromCache(String userId, final Handler<JsonObject> handler) {
		final JsonObject matcher = new JsonObject().putString("user", userId);


		mongo.findOne(CACHE_COLLECTION, matcher, MongoDbResult.validResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					handler.handle(event.right().getValue());
				} else {
					log.error(event.left().getValue());
					handler.handle(null);
				}
			}
		}));
	}

	@Override
	public void storeInCache(String userId, JsonArray ja, final Handler<Boolean> handler) {
		final JsonObject joCache = new JsonObject();
		joCache.putString("user", userId);
		joCache.putObject("insertedAt", MongoDb.now());
		joCache.putArray("results", ja);

		mongo.insert(CACHE_COLLECTION, joCache, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					handler.handle(Boolean.TRUE);
				} else {
					log.error(event.body().getString("message"));
					handler.handle(Boolean.FALSE);
				}
			}
		});
	}

	private String generateCasJson(String userId, String pronoteContext, JsonArray jaCAS, JsonObject joApp) {
		//due to the checking implementation of the service, generating a ST by url (ST service == TARGET Proxy service)
		final JsonObject joCAS = new JsonObject();
		joCAS.putString("id", UUID.randomUUID().toString());
		joCAS.putString("user", userId);
		joCAS.putBoolean("loggedIn", Boolean.TRUE);
		joCAS.putObject("updatedAt", MongoDb.now());

		//ST
		final JsonObject joST = new JsonObject();
		joST.putString("ticketParameter", "ticket");
		joST.putString("service", joApp.getString("app.address") + pronoteContext);
		joST.putString("ticket", "ST-" + UUID.randomUUID().toString());
		joST.putValue("issued", System.currentTimeMillis());
		joST.putBoolean("used", Boolean.TRUE);

		//PGT
		final JsonObject joPGT = new JsonObject();
		joPGT.putString("pgtId", "PGT-" + UUID.randomUUID().toString());
		joPGT.putString("pgtIOU", "PGTIOU-" + UUID.randomUUID().toString());
		joPGT.putArray("pgtUrls", new JsonArray(new String[]{FAKE_PGT_URL}));

		//PT
		final JsonObject joPT = new JsonObject();
		final String pt = "PT-" + UUID.randomUUID().toString();
		joPT.putString("pgId", pt);
		joPT.putBoolean("used", Boolean.FALSE);
		joPT.putValue("issued", System.currentTimeMillis());

		//add PT to PGT object
		final JsonArray jaPT = new JsonArray();
		jaPT.addObject(joPT);
		joPGT.putArray("proxyTickets", jaPT);

		//add PGT to ST object
		joST.putObject("pgt", joPGT);

		//add ST to cas object
		final JsonArray jaST = new JsonArray();
		jaST.addObject(joST);
		joCAS.putArray("serviceTickets", jaST);


		jaCAS.addObject(joCAS);

		return pt;
	}
}