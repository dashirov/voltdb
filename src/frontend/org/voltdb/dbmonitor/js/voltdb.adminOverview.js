﻿(function ($) {
    var adminOverviewFunctions = function (options) {
        this.options = options;
        
        this.displayAdminConfiguration = function (adminConfigValues) {
            configureAdminValues(adminConfigValues);
            configurePortValues(adminConfigValues);
            configureDirectoryValues(adminConfigValues);
        };

        this.displayClientPort = function (clientPortValue) {
            configureClientPortValue(clientPortValue);
        };   

        var configureAdminValues = function (adminConfigValues) {
            options.siteNumberHeader.text(adminConfigValues.sitesperhost);
            options.kSafety.text(adminConfigValues.kSafety);
            options.partitionDetection.className = adminConfigValues.partitionDetection == 'true' ? 'onIcon' : 'offIcon';
            options.httpAccess.className = adminConfigValues.httpEnabled == 'true' ? 'onIcon' : 'offIcon';
            options.jsonAPI.className = adminConfigValues.jsonEnabled == 'true' ? 'onIcon' : 'offIcon';
            options.jsonAPI.className = adminConfigValues.snapshotEnabled == 'true' ? 'onIcon' : 'offIcon';
            options.commandLog.className = adminConfigValues.commandLogEnabled == 'true' ? 'onIcon' : 'offIcon';
            //options.commandLogFrequencyTime.text(adminConfigValues.commandLogFrequencyTime);
            //options.commandLogFrequencyTransactions.text(adminConfigValues.commandLogFrequencyTransactions);
            options.heartBeatTimeout.text(adminConfigValues.heartBeatTimeout);
            options.tempTablesMaxSize.text(adminConfigValues.tempTablesMaxSize);
            options.snapshotPriority.text(adminConfigValues.snapshotPriority);

        };

        var configurePortValues = function (portConfigValues) {
            options.adminPort.text(portConfigValues.adminPort);
            options.httpPort.text(portConfigValues.httpPort);
            options.internalPort.text(portConfigValues.internalPort);
            options.zookeeperPort.text(portConfigValues.zookeeperPort);
            options.replicationPort.text(portConfigValues.replicationPort);
        };

        var configureClientPortValue = function (clientPortValue) {
            options.clientPort.text(clientPortValue.clientPort);
        };

        var configureDirectoryValues = function (directoryConfigValues) {
            options.root.text(directoryConfigValues.voltdbRoot);
            options.snapShot.text(directoryConfigValues.snapshotPath);
            options.commandLogs.text(directoryConfigValues.commandLogPath);
            options.commandLogSnapShots.text(directoryConfigValues.commandLogSnapshotPath);

        };

    };

    $.fn.adminOverview = function (option) {
        // set defaults
        var adminDOMObjects = {
            siteNumberHeader: $("#sitePerHost"),
            kSafety: $("#kSafety"),
            partitionDetection: $("#partitionDetectionIcon"),
            httpAccess: $("#httpAccessIcon"),
            jsonAPI: $("#jsonAPIIcon"),
            autoSnapshot: $("#autoSnapshotIcon"),
            commandLog: $("#commandLogIcon"),
            commandLogFrequencyTime: $("#commandlogfreqtime"),
            commandlogFrequencyTransactions: $("#commandlogfreqtxns"),
            heartBeatTimeout: $("#hrtTimeOutSpan"),
            tempTablesMaxSize: $("#temptablesmaxsize"),
            snapshotPriority: $("#snapshotpriority"),
            clientPort: $('#clientport'),
            adminPort: $('#adminport'),
            httpPort: $('#httpport'),
            internalPort: $('#internalPort'),
            zookeeperPort: $('#zookeeperPort'),
            replicationPort: $('#replicationPort'),
            root: $('#voltdbroot'),
            snapShot: $('#snapshotpath'),
            commandLogs: $('#commandlogpath'),
            commandLogSnapShots: $('#commandlogsnapshotpath')

        };

        var options = $.extend({}, adminDOMObjects, option);

        //write blocks and function calls that are to be executed during load operation
        $(document).ready(function () {
            //site per host           
            

        });
        return new adminOverviewFunctions(options);

    };


})(jQuery);