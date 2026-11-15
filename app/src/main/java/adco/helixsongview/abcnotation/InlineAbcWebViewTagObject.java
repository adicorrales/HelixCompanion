package adco.helixsongview.abcnotation;

public class InlineAbcWebViewTagObject {
    private int objectNumber = -1;
    private int containingViewNumber = -1;

    // This is used to set some tag information for WebViews
    public void setObjectNumber(int objectNumber) {
        this.objectNumber = objectNumber;
    }

    public void setContainingViewNumber(int containingViewNumber) {
        this.containingViewNumber = containingViewNumber;
    }

}
