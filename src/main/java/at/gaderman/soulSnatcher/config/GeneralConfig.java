package at.gaderman.soulSnatcher.config;

public class GeneralConfig {

    private static GeneralConfig instance;

    private GeneralConfig() {
        setUp();
    }

    public static GeneralConfig getInstance() {
        if (instance == null) instance = new GeneralConfig();
        return instance;
    }

    private void setUp(){

    }

}
