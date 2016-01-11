package fr.wseduc.sso.services.keyring.impl;

import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.validation.StringValidation;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.sql.SqlResult.*;

public class DefaultKeyRingService implements KeyRingService {

	private Sql sql = Sql.getInstance();

	@Override
	public void create(final JsonObject data, final Handler<Either<String, JsonObject>> handler) {
		final String tableName;
		if (data.getString("name") != null) {
			tableName = StringValidation.removeAccents(data.getString("name").trim()
					.replaceAll("\\s+", "_")).replaceAll("[^\\p{Alpha}\\p{Digit}_]+", "").toLowerCase();
		} else {
			handler.handle(new Either.Left<String, JsonObject>("invalid.name"));
			return;
		}
		sql.raw("select nextval('sso.keyring_id_seq') as next_id", validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					final Long id = event.right().getValue().getLong("next_id");
					final String table = tableName + "_" + id;
					data.putNumber("id", id);
					data.putString("service_id", table);
					JsonObject formSchema = data.getObject("form_schema");
					String s = createServiceQuery(table, formSchema);
					SqlStatementsBuilder sb = new SqlStatementsBuilder();
					sb.insert("sso.keyring", data, "id, service_id");
					sb.raw(s);
					sql.transaction(sb.build(), validUniqueResultHandler(0, handler));
				} else {
					handler.handle(event);
				}
			}
		}));
	}

	private String createServiceQuery(String tableName, JsonObject formSchema) {
		StringBuilder s = new StringBuilder()
				.append("CREATE TABLE sso.").append(tableName).append(" ( ")
				.append("id BIGSERIAL PRIMARY KEY, ")
				.append("user_id VARCHAR(50) NOT NULL UNIQUE");
		for (String attr: formSchema.getFieldNames()) {
			s.append(", ").append(attr).append(" VARCHAR(256) NOT NULL");
		}
		s.append(");");
		return s.toString();
	}

	@Override
	public void alter(final String serviceId, final JsonObject data, final Handler<Either<String, JsonObject>> handler) {
		StringBuilder sb = new StringBuilder();
		final JsonArray values = new JsonArray();
		for (String attr : data.getFieldNames()) {
			sb.append(attr).append(" = ?, ");
			values.add(data.getValue(attr));
		}
		sb.deleteCharAt(sb.length() - 2);
		values.add(serviceId);
		final String query = "UPDATE sso.keyring SET " + sb.toString() + "WHERE service_id = ? ";
		if (data.containsField("form_schema")) {
			description(serviceId, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> r) {
					if (r.isRight()) {
						JsonObject cfs = r.right().getValue().getObject("form_schema");
						JsonObject rfs = data.getObject("form_schema");
						if (rfs != null && !rfs.equals(cfs)) {
							SqlStatementsBuilder sb = new SqlStatementsBuilder();
							sb.raw(dropServiceQuery(serviceId));
							sb.raw(createServiceQuery(serviceId, rfs));
							sb.prepared(query, values);
							sql.transaction(sb.build(), validRowsResultHandler(2, handler));
						}
					} else {
						handler.handle(r);
					}
				}
			});
		} else {
			sql.prepared(query, values, validRowsResultHandler(handler));
		}
	}

	private String dropServiceQuery(String serviceId) {
		return "DROP TABLE sso." + serviceId;
	}

	@Override
	public void drop(String serviceId, Handler<Either<String, JsonObject>> handler) {
		SqlStatementsBuilder sb = new SqlStatementsBuilder();
		sb.prepared("DELETE FROM sso.keyring WHERE service_id = ?", new JsonArray().add(serviceId));
		sb.raw(dropServiceQuery(serviceId));
		sql.transaction(sb.build(), validRowsResultHandler(handler));
	}

	@Override
	public void description(String serviceId, Handler<Either<String, JsonObject>> handler) {
		sql.prepared(
				"SELECT id, service_id, name, url, description, form_schema " +
				"FROM sso.keyring " +
				"WHERE service_id = ? ",
				new JsonArray().add(serviceId), validUniqueResultHandler(handler, "form_schema"));
	}


	@Override
	public void listServices(Handler<Either<String, JsonArray>> handler) {
		sql.select("sso.keyring", new JsonArray().add("id").add("service_id").add("name")
				.add("url").add("description").add("form_schema"), validResultHandler(handler, "form_schema"));
	}

}
