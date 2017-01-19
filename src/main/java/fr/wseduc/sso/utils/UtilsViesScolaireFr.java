/**
 * Created by vogelmt on 07/12/2016.
 */
package fr.wseduc.sso.utils;

/**
 * @author MVOG
 */
public class UtilsViesScolaireFr {

    public static String URL_KEY_CRYPT = "/vsn.main/?extautolog=";

    public static String ENT_PERSONNE_JOINTURE_KEY = "entPersonneJointure=";

    public static String APPLI_KEY = "&appli=";

    public static String PROFIL_KEY = "&profil=";
    
    public static String NOM_KEY = "&nom=";

    public static String PRENOM_KEY = "&prenom=";

    public static String DTN_KEY = "&dtn=";

    public static String SERVICE_TICKET_KEY = "&ticket=";

    public static String URL_PROPERTY_END_LVS = "URL_PROPERTY_END_LVS";
    
    private static String PROFIL_ELEVE = "eleve";

    private static String PROFIL_PROFESSEUR = "professeur";

    private static String PROFIL_RESPONSABLE = "responsable";

    private static String PROFIL_PERSONNE = "personne";

    public static String CRYPTAGE_ALGORITHME = "RSA";


    public static final String PROFIL_ELEVE_ENT = "Student";
    public static final String PROFIL_ENSEIGNANT_ENT = "Teacher";
    public static final String PROFIL_PERSONNE_REL_ELEVE_ENT = "Relative";


    /**
     * Convert ENT profile name to VieScolaire.Fr profile name
     * @param profilStr ENT profile name
     * @return VieScolaire.Fr profile
     */
    public static String getProfilVieScolaireFr(String profilStr) {
        if (profilStr.toLowerCase().equals(PROFIL_ELEVE_ENT.toLowerCase())) {
            return PROFIL_ELEVE;
        } else if (profilStr.toLowerCase().equals(PROFIL_ENSEIGNANT_ENT.toLowerCase())) {
            return PROFIL_PROFESSEUR;
        } else if (profilStr.toLowerCase().equals(PROFIL_PERSONNE_REL_ELEVE_ENT.toLowerCase())) {
            return PROFIL_RESPONSABLE;
        }

        return PROFIL_PERSONNE;
    }
}
