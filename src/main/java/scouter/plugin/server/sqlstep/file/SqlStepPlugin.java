package scouter.plugin.server.sqlstep.file;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.TextTypes;
import scouter.lang.pack.*;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.lang.step.*;
import scouter.plugin.server.sqlstep.file.payload.SQLStep;
import scouter.plugin.server.sqlstep.file.payload.JDBCLogging;
import scouter.plugin.server.sqlstep.file.payload.SQLStepWrapper;
import scouter.plugin.server.sqlstep.file.payload.ServiceLogging;
import scouter.server.ConfObserver;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.server.db.XLogProfileRD;
import scouter.server.plugin.PluginHelper;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Heo Yeo Song (yosong.heo@gmail.com) on 2021. 3. 18.
 */
@Slf4j
public class SqlStepPlugin {



    Configure conf = Configure.getInstance();
    private static final String ext_plugin_sqlstep_enabled          = "ext_plugin_sqlstep_enabled";
    private static final String ext_plugin_sqlstep_index            = "ext_plugin_sqlstep_index";
    private static final String ext_plugin_sqlstep_duration_day     = "ext_plugin_sqlstep_duration_day";
    private static final String ext_plugin_sqlstep_root_dir         = "ext_plugin_sqlstep_root_dir";
    private static final String ext_plugin_sqlstep_rotate_dir       = "ext_plugin_sqlstep_rotate_dir";
    private static final String ext_plugin_sqlstep_extension        = "ext_plugin_sqlstep_extension";
    private static final String ext_plugin_sqlstep_debug_enabled        = "ext_plugin_sqlstep_debug_enabled";


    private final FileScheduler sqlStepFileScheduler;


    final Map<String,FileLogRotate> couterMagement;
    final PluginHelper helper;
    boolean enabled;                                              
    String name;
    int stepDuration;
    FileLogRotate xlogLoggerJSON;
    FileLogRotate xlogLoggerCSV;
    String rootDir;
    String moveDir;
    String extension;
    
    final DateTimeFormatter dateTimeFormatter;
    private final boolean _isDebug;


    public SqlStepPlugin() {

        this.dateTimeFormatter  = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSZ").withZone(ZoneId.systemDefault());
        this.helper             = PluginHelper.getInstance();
        this.enabled            = conf.getBoolean(ext_plugin_sqlstep_enabled, true);
        this.name               = conf.getValue(ext_plugin_sqlstep_index, "sqltstep-xlog");
        this.stepDuration       = conf.getInt(ext_plugin_sqlstep_duration_day, 3);
        this.rootDir            = conf.getValue(ext_plugin_sqlstep_root_dir, "./ext_plugin_sqlstep");
        this.moveDir            = conf.getValue(ext_plugin_sqlstep_rotate_dir, "./ext_plugin_sqlstep/rotate");
        this.extension          = conf.getValue(ext_plugin_sqlstep_extension, "json");
        this._isDebug           = conf.getBoolean(ext_plugin_sqlstep_debug_enabled, false);

        this.couterMagement     = new ConcurrentHashMap<>();
        this.xlogLoggerJSON = new FileLogRotate(this.name,"json",this.rootDir,this.moveDir);
        this.xlogLoggerJSON.create();

        this.xlogLoggerCSV         = new FileLogRotate(this.name,"csv",this.rootDir,this.moveDir);
        this.xlogLoggerCSV.create();



        //- 스케줄 정의
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,24);
        calendar.set(Calendar.MINUTE,0);
        final Date schStartTime= new Date(calendar.getTimeInMillis());


        this.sqlStepFileScheduler = new FileScheduler(this.moveDir
                ,this.name
                ,"filerotate-scheduler-sqlstep"
                ,schStartTime
                , stepDuration
                , dateTimeFormatter
        );
        this.sqlStepFileScheduler.start();

        ConfObserver.put("SQLSTEP-ServerPluginLogPlugin", ()-> {
            enabled            = conf.getBoolean(ext_plugin_sqlstep_enabled, true);
            stepDuration = conf.getInt(ext_plugin_sqlstep_duration_day, 3);
            sqlStepFileScheduler.setDuration(stepDuration);
            Logger.println("reload sqlstep enabled result : " + enabled);
        });
        log.info("PLUG-IN: SQLStep starting ...");
    }

    private void save(final XLogPack p,final List<SQLStep> h) {
        if (!enabled) {
            return;
        }

        ObjectPack op= AgentManager.getAgent(p.objHash);
        if(Objects.isNull(op)){
                return;
        }

        try {
            for(SQLStep jh : h ) {
                xlogLoggerJSON.execute(JDBCLogging.builder()
                        .name(op.objName)
                        .url(this.getString(helper.getServiceString(p.service)))
                        .requestTime(this.dateTimeFormatter.format(new Date(p.endTime - p.elapsed).toInstant()))
                        .error(helper.getErrorString(p.error))
                        .txid(Hexa32.toString32(p.txid))
                        .gxid(p.gxid != 0 ? Hexa32.toString32(p.gxid) : null)
                        .history(jh)
                        .build(), this._isDebug);
            }

            ServiceLogging serviceLogging = ServiceLogging.builder()
                    .name(op.objName)
                    .url(this.getString(helper.getServiceString(p.service)))
                    .requestTime(this.dateTimeFormatter.format(new Date(p.endTime - p.elapsed).toInstant()))
                    .error(helper.getErrorString(p.error))
                    .txid(Hexa32.toString32(p.txid))
                    .gxid(p.gxid != 0 ? Hexa32.toString32(p.gxid) : null)
                    .elapsed(p.elapsed)
                    .sqlCallCount(p.sqlCount)
                    .sqlCallTime(p.sqlTime)
                    .apiCallCount(p.apicallCount)
                    .apiCallTime(p.apicallTime)
                    .history(SQLStepWrapper.builder().steps(h).type("service").build())
                    .build();

            xlogLoggerJSON.execute(serviceLogging,this._isDebug);
            xlogLoggerCSV.execute(serviceLogging,this._isDebug);
        }catch (IOException e){
                log.error("{}",e);
        }
    }

    private String getString(String value){
        return StringUtil.nullToEmpty(value);
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_XLOG)
    public void xlog(final XLogPack p) {
        byte[] profile = XLogProfileRD.getProfile(DateUtil.yyyymmdd(), p.txid, 10000);
        if(Objects.isNull(profile)){
            log.warn(" profile  is not search {}", p.txid);
            return;
        }
        try {

            Step[] steps = Step.toObjects(profile);
            if (Objects.nonNull(steps)) {
                List<SQLStep> sqlSteps = new ArrayList<>();
                for (Step step : steps) {
                    StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
                    switch (stepType) {
                        case METHOD:
                            MethodStep methodStep = (MethodStep) step;
                            String open = helper.getMethodString(methodStep.hash);
                            if (open.indexOf("OPEN-DBC") > -1) {
//                                builder.open(open);
                                SQLStep.SQLStepBuilder openStep = SQLStep.builder();
                                openStep.open(open);
                                openStep.type("open");
                                sqlSteps.add(openStep.build());
                            }
                            break;
                        case METHOD2:
                            MethodStep2 methodStep2 = (MethodStep2) step;
                            String apError = helper.getErrorString(methodStep2.error);
                            SQLStep.SQLStepBuilder apErrorStep = SQLStep.builder();
                            if (StringUtil.isNotEmpty(apError)) {
                                apErrorStep.apError(apError);
                                apErrorStep.type("error");
                                sqlSteps.add(apErrorStep.build());
                            }

                            break;
                        case SQL:
                        case SQL2:
                        case SQL3:
                            SQLStep.SQLStepBuilder sqlStep = SQLStep.builder();
                            SqlStep sql = (SqlStep) step;
                            sqlStep.param(sql.param);
                            sqlStep.tables(TextRD.getString(DateUtil.yyyymmdd(), TextTypes.SQL_TABLES, sql.hash));
                            sqlStep.sql(helper.getSqlString(sql.hash));
                            sqlStep.sqlError(helper.getErrorString(sql.error));
                            sqlStep.elapsed(sql.elapsed);
                            sqlStep.type("sql");
                            sqlSteps.add(sqlStep.build());
                            break;
                    }
                }
                //-call
                this.save(p,sqlSteps);
            }
        }catch (Throwable e){
            log.error("read profile db failed {}",e);
        }
    }

}