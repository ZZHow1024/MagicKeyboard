module com.zzhow.magickeyboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.sun.jna;
    requires com.sun.jna.platform;


    opens com.zzhow.magickeyboard to javafx.fxml;
    exports com.zzhow.magickeyboard;
    exports com.zzhow.magickeyboard.controller;
    opens com.zzhow.magickeyboard.controller to javafx.fxml;
    exports com.zzhow.magickeyboard.window;
    opens com.zzhow.magickeyboard.window to javafx.fxml;
    exports com.zzhow.magickeyboard.core.impl;
}