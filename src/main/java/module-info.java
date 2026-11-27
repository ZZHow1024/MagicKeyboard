module com.zzhow.magickeyboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.logging;
    requires java.prefs;

    // JavaFX LauncherImpl 通过反射构造 Application 子类，需要定向导出
    exports com.zzhow.magickeyboard to javafx.graphics;
    exports com.zzhow.magickeyboard.window to javafx.graphics;

    opens com.zzhow.magickeyboard to javafx.fxml;
    opens com.zzhow.magickeyboard.controller to javafx.fxml;
    opens com.zzhow.magickeyboard.window to javafx.fxml;
}
