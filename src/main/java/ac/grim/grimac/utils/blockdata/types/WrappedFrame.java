package ac.grim.grimac.utils.blockdata.types;

public class WrappedFrame extends WrappedBlockDataValue {
    boolean hasEye = false;

    public void setHasEye(boolean hasEye) {
        this.hasEye = hasEye;
    }

    public boolean hasEye() {
        return hasEye;
    }
}
