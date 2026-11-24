package adco.helixsongview.interfaces;

public interface DisplayInterface {

    // General display tasks (from any mode)
    void checkDisplays();
    void updateDisplay(String what);

    boolean getIsSecondaryDisplaying();

}
