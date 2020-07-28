package src.test.resources;
@Deprecated
module moduletags {
    requires transitive static moduleA;

    exports testpkgmdltags;
}