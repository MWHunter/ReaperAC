package ac.grim.grimac.utils.blockdata.types;

public class WrappedPiston extends WrappedDirectional {
    boolean isShort = false;

    public boolean isShort() {
        return isShort;
    }

    public void setShort(boolean isShort) {
        this.isShort = isShort;
    }
}
