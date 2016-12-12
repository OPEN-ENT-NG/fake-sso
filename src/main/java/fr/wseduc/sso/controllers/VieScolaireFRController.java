package fr.wseduc.sso.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.sso.utils.UtilsViesScolaireFr;
import fr.wseduc.webutils.Utils;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import javax.crypto.Cipher;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by vogelmt on 07/12/2016.
 */
public class VieScolaireFRController extends SSOController {

    private static final Logger log = LoggerFactory.getLogger(VieScolaireFRController.class);
    private PublicKey viescolairefrPublicKey;
    private String appli;
    private boolean configOk = false;
    private String urlEndLVS ;
    private int responseTimeout;
    private static final String patternDateLVS = "dd/MM/yyyy";
    SimpleDateFormat simpleDateFormat;
    DateFormat patternDateNG = new SimpleDateFormat("yyyy-MM-dd");
    private static final String parameterRequestEtabURL = "url";

    @Get("/viescolairefr")
    @SecuredAction(value = "viescolairefr", type = ActionType.AUTHENTICATED)
    public void access(final HttpServerRequest request) {
        final AtomicBoolean responseIsSent = new AtomicBoolean(false);
        if (!configOk) {
            log.error("Invalid VieScolaireFR configuration");
            renderError(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final String urlVieScolaireFREtablissement = request.params().get(parameterRequestEtabURL);
                    if(null != urlVieScolaireFREtablissement
                            && Utils.isNotEmpty(urlVieScolaireFREtablissement)){

                        StringBuilder urlToGetTicketStrBuilder = new StringBuilder(urlVieScolaireFREtablissement);

                        URI viescolairefrURI = null;
                        try {
                            viescolairefrURI = new URI(urlToGetTicketStrBuilder.toString());
                        } catch (URISyntaxException e) {
                            log.error("Invalid viescolairefr web service uri", e);
                            renderError(request);
                        }

                        if (viescolairefrURI != null) {
                            urlToGetTicketStrBuilder.append(urlEndLVS);
                            final HttpClient httpClient = generateHttpClient(viescolairefrURI);
                            log.debug("Get VSFR ticket : " + user.getUserId());
                            final HttpClientRequest httpClientRequest = httpClient.get(urlToGetTicketStrBuilder.toString()  , new Handler<HttpClientResponse>() {
                                @Override
                                public void handle(HttpClientResponse response) {
                                    if (response.statusCode() == 200) {
                                        final Buffer ticketVSFRBuffer = new Buffer();
                                        response.dataHandler(new Handler<Buffer>() {
                                            @Override
                                            public void handle(Buffer event) {
                                                ticketVSFRBuffer.appendBuffer(event);
                                                String ticketVSFR = ticketVSFRBuffer.toString();
                                                if( null != ticketVSFR
                                                        && !ticketVSFR.isEmpty()){
                                                    log.debug("Building VSFR URL : " + user.getUserId());

                                                    // Building unencrypted url
                                                    String userParametre = constructURLVieScolaireFR(ticketVSFR, user);

                                                    log.debug("VSFR URL builded :  userParametre -> " + userParametre);

                                                    // Building crypted URL
                                                    try {
                                                        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                                        cipher.init(Cipher.ENCRYPT_MODE, viescolairefrPublicKey);
                                                        final String urlCrypte = URLEncoder.encode(Base64.encodeBytes(
                                                                cipher.doFinal(userParametre.getBytes("UTF-8"))), "UTF-8");

                                                        String urIAppel = UtilsViesScolaireFr.URL_KEY_CRYPT + urlCrypte;

                                                        log.debug("Crypted VSFR URL build : " + urIAppel);
                                                        // REDIRECTION vers VSFR
                                                        redirect(request, urlVieScolaireFREtablissement, urIAppel);
                                                    } catch (Exception e) {
                                                        log.error("Error encrypting rsa viescolairefr url");
                                                        renderError(request);
                                                    }
                                                } else{
                                                    log.error("Empty VSFR ticket : " + user.getUserId());
                                                    renderError(request);
                                                }
                                            }
                                        });
                                    } else {
                                        log.error("Error when calling VSFR URL to get ticket : " + response.statusMessage());
                                        renderError(request);
                                    }
                                    if (!responseIsSent.getAndSet(true)) {
                                        httpClient.close();
                                    }
                                }
                            });
                            httpClientRequest.headers().set("Content-Length", "0");
                            httpClientRequest.setTimeout(responseTimeout);
                            //Typically an unresolved Address, a timeout about connection or response
                            httpClientRequest.exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable event) {
                                    log.error("Error when calling VSFR URL to get ticket for user : " + user.getUserId(), event);
                                    if (!responseIsSent.getAndSet(true)) {
                                        httpClient.close();
                                    }
                                    renderError(request);
                                }
                            }).end();
                        }
                    }else{
                        log.error("Error : URL in parameters is  empty or undefined for user : " + user.getUserId());
                        renderError(request);
                    }

                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /***
     * Build redirect URL to laviescolaire.fr from the current user
     * Example : entPersonneJointure=[entPersonneJointure]&appli=[appli]&profil=[profil]&nom=[nom]&prenom=[prenom]&dtn=[dtn]&ticket=[ticket]
     * @param ticketVSFR ticket specified in the URL
     * @param user current user
     * @return parsed URL
     */
    private String constructURLVieScolaireFR(String ticketVSFR, UserInfos user) {

        String personneJointureIdStr = user.getUserId();
        String profilVSFR = UtilsViesScolaireFr.getProfilVieScolaireFr(user.getType());
        StringBuffer userParametreBuffer = new StringBuffer(UtilsViesScolaireFr.ENT_PERSONNE_JOINTURE_KEY + personneJointureIdStr
                + UtilsViesScolaireFr.APPLI_KEY + appli
                + UtilsViesScolaireFr.PROFIL_KEY + profilVSFR);

        if (null != user){
            if(null != user.getLastName()
                    && Utils.isNotEmpty(user.getLastName())){
                userParametreBuffer.append(UtilsViesScolaireFr.NOM_KEY + user.getLastName());
            }else{
                userParametreBuffer.append(UtilsViesScolaireFr.NOM_KEY);
            }
            if(null != user.getFirstName()
                    && Utils.isNotEmpty(user.getFirstName())){
                userParametreBuffer.append(UtilsViesScolaireFr.PRENOM_KEY + user.getFirstName());
            }else {
                userParametreBuffer.append(UtilsViesScolaireFr.PRENOM_KEY);
            }
            if(null != user.getBirthDate()){
                String newDateOfBirthday = "";
                try {
                    Date dateOfBirthday = patternDateNG.parse(user.getBirthDate());
                    newDateOfBirthday = simpleDateFormat.format(dateOfBirthday);
                } catch (ParseException e) {
                    log.error("Error when parsing birthdate for user : " + user.getBirthDate(), e);
                }
                if(Utils.isNotEmpty(newDateOfBirthday)){
                    userParametreBuffer.append(UtilsViesScolaireFr.DTN_KEY + newDateOfBirthday);
                } else {
                    userParametreBuffer.append(UtilsViesScolaireFr.DTN_KEY);
                }
            } else {
                userParametreBuffer.append(UtilsViesScolaireFr.DTN_KEY);
            }
        }else{
            userParametreBuffer.append(UtilsViesScolaireFr.NOM_KEY);
            userParametreBuffer.append(UtilsViesScolaireFr.PRENOM_KEY);
            userParametreBuffer.append(UtilsViesScolaireFr.DTN_KEY);
        }
        userParametreBuffer.append(UtilsViesScolaireFr.SERVICE_TICKET_KEY + ticketVSFR);

        return userParametreBuffer.toString();
    }

    /**
     * Generate HTTP client
     * @param uri uri
     * @return Http client
     */
    private HttpClient generateHttpClient(URI uri) {
        return vertx.createHttpClient()
                .setHost(uri.getHost())
                .setPort((uri.getPort() > 0) ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80))
                .setVerifyHost(false)
                .setTrustAll(true)
                .setSSL("https".equals(uri.getScheme()))
                .setKeepAlive(false);
    }

    /**
     * Set SSO configuration from the mod configuration
     * @param ssoConfig Mod configuration
     */
    @Override
    public void setSsoConfig(JsonObject ssoConfig) {
        if (ssoConfig == null || ssoConfig.getString("public-key", "").isEmpty()
                ||  ssoConfig.getString("appli", "").isEmpty()
                ||  ssoConfig.getString(UtilsViesScolaireFr.URL_PROPERTY_END_LVS, "").isEmpty()) {
            log.error("Invalid VieScolaireFR configuration");
        } else {

            appli = ssoConfig.getString("appli");
            responseTimeout = ssoConfig.getInteger("connection-timeout");
            urlEndLVS = ssoConfig.getString(UtilsViesScolaireFr.URL_PROPERTY_END_LVS);
            simpleDateFormat = new SimpleDateFormat(patternDateLVS);

            String publicKey = ssoConfig.getString("public-key");
            if (Utils.isNotEmpty(publicKey)) {
                try {
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKey));
                    KeyFactory kf = KeyFactory.getInstance(UtilsViesScolaireFr.CRYPTAGE_ALGORTIHME);
                    viescolairefrPublicKey = kf.generatePublic(spec);
                    configOk = true;
                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
    }
}
