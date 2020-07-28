package src.test.resources;
public class AnonInnerDefaults {

    public void tryStuff(final UIElement e) {
        PolyIface p = new @UI PolyIface() {};
    }
}
