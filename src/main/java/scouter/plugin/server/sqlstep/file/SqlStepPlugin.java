package scouter.plugin.server.sqlstep.file;

import lombok.extern.slf4j.Slf4j;
import scouter.io.DataInputX;
import scouter.lang.ObjectType;
import scouter.lang.TextTypes;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.*;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.lang.step.*;
import scouter.lang.value.Value;
import scouter.server.ConfObserver;
import scouter.server.Configure;
import scouter.server.CounterManager;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.server.db.XLogRD;
import scouter.server.plugin.PluginHelper;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

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


    private final FileScheduler sqlStepFileScheduler;

    final Map<String,FileLogRotate> couterMagement;
    final PluginHelper helper;
    boolean enabled;                                              
    String couterIndexName;
    String name;
    int counterDuration;
    int stepDuration;
    FileLogRotate xlogLogger;
    String rootDir;
    String moveDir;
    String extension;
    
    final DateTimeFormatter dateTimeFormatter;



    public SqlStepPlugin() {

        this.dateTimeFormatter  = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSZ").withZone(ZoneId.systemDefault());
        this.helper             = PluginHelper.getInstance();
        this.enabled            = conf.getBoolean(ext_plugin_sqlstep_enabled, true);
        this.name               = conf.getValue(ext_plugin_sqlstep_index, "sqltstep-xlog");
        this.stepDuration       = conf.getInt(ext_plugin_sqlstep_duration_day, 3);
        this.rootDir            = conf.getValue(ext_plugin_sqlstep_root_dir, "./ext_plugin_sqlstep");
        this.moveDir            = conf.getValue(ext_plugin_sqlstep_rotate_dir, "./ext_plugin_sqlstep/rotate");
        this.extension          = conf.getValue(ext_plugin_sqlstep_extension, "json");

        this.couterMagement     = new ConcurrentHashMap<>();
        this.xlogLogger         = new FileLogRotate(this.name,this.extension,this.rootDir,this.moveDir);
        this.xlogLogger.create();


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
                ,dateTimeFormatter
        );
        this.sqlStepFileScheduler.start();

        ConfObserver.put("SQLSTEP-ServerPluginLogPlugin", ()-> {
            enabled            = conf.getBoolean(ext_plugin_sqlstep_enabled, true);
            stepDuration = conf.getInt(ext_plugin_sqlstep_duration_day, 3);
            sqlStepFileScheduler.setDuration(stepDuration);
            Logger.println("ServerPluginFileLogPlugin Enabled Result : " + enabled);
        });
        log.info("PLUG-IN: starting sqlStep Plugin...");
    }


    @ServerPlugin(PluginConstants.PLUGIN_SERVER_COUNTER)
    public void counter(final PerfCounterPack pack) {

        if (!enabled) {
            return;
        }

        if(pack.timetype != TimeTypeEnum.REALTIME) {
            return;
        }
        try {
            String objName = pack.objName;
            int objHash    = HashUtil.hash(objName);
            ObjectPack op  = AgentManager.getAgent(objHash);
            if(Objects.isNull(op)){
                return;
            }
            ObjectType objectType = CounterManager.getInstance().getCounterEngine().getObjectType(op.objType);
            String objFamily = objectType.getFamily().getName();

            Map<String, Value> dataMap = pack.data.toMap();
            Map<String,Object> _source = new LinkedHashMap<>();
            _source.put("server_id",this.conf.server_id);
            _source.put("startTime", this.dateTimeFormatter.format(new Date(pack.time).toInstant()));
            _source.put("objName",op.objName);
            _source.put("objHash",Hexa32.toString32(objHash));
            _source.put("objType",op.objType);
            _source.put("objFamily",objFamily);

            for (Map.Entry<String, Value> field : dataMap.entrySet()) {
                Value valueOrigin = field.getValue();
                if (Objects.isNull(valueOrigin)) {
                    continue;
                }
                Object value = valueOrigin.toJavaObject();
                if(!(value instanceof Number)) {
                    continue;
                }
                String key = field.getKey();
                if(Objects.equals("time",key) || Objects.equals("objHash",key)) {
                    continue;
                }
                _source.put(key,value);
            }

            this.getCounterLogger(objFamily).execute(_source);
        } catch (Exception e) {
            Logger.printStackTrace("counter logging failed", e);
        }
    }
    private FileLogRotate getCounterLogger(String objFamily) {
        return Optional.ofNullable(this.couterMagement.get(objFamily))
                       .orElseGet(()->{
                           FileLogRotate fileLogRotate=  new FileLogRotate(
                                                            String.join("-",this.couterIndexName,objFamily)
                                                            , this.extension
                                                            , this.rootDir
                                                            , this.moveDir);
                           if(fileLogRotate.create()){
                             this.couterMagement.put(objFamily,fileLogRotate);
                           }
                           return fileLogRotate;
                       });

    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_PROFILE)
    public void profile(final XLogProfilePack m) {
        try {
            byte[] stepHash = m.profile;

            Step[] steps = Step.toObjects(stepHash);
            if (Objects.nonNull(steps)) {
                for (Step step : steps) {
                    StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
                    switch (stepType) {
                        case METHOD:
                            MethodStep methodStep = (MethodStep) step;
                            String open= helper.getMethodString(methodStep.hash);
                            if(open.indexOf("OPEN-DBC") > -1) {
                                log.info("{}", open);
                            }
                            break;
                        case METHOD2:
                            MethodStep2 methodStep2 = (MethodStep2) step;
                            log.info("{}", helper.getErrorString(methodStep2.error));
                            break;
                        case SQL:
                        case SQL2:
                        case SQL3:

                            SqlStep sql = (SqlStep) step;
                            log.info("{} {} {} {}",
                                    helper.getSqlString(sql.hash),
                                    sql.param,
                                    helper.getErrorString(sql.error),
                                    TextRD.getString(DateUtil.yyyymmdd(), TextTypes.SQL_TABLES, sql.hash));
                            break;
                        case SQL_SUM:
                            SqlSum sqlSum = (SqlSum)step;
                            log.info("{}",helper.getSqlString(sqlSum.hash));
                            break;

                    }
                }
            }
            byte[] read = XLogRD.getByTxid(DateUtil.yyyymmdd(), m.txid);
            if (Objects.nonNull(read)) {
                Pack pack = new DataInputX(read).readPack();
                XLogPack xlog = (XLogPack) pack;
                this.xlog(xlog);
            }

        } catch (Throwable e) {
            log.error("profile parsing error", e);
        }

    }
    public void xlog(final XLogPack p) {
        if (!enabled) {
            return;
        }
//        try {
            Map<String,Object> _source = new LinkedHashMap<>();
            ObjectPack op= AgentManager.getAgent(p.objHash);

            if(Objects.isNull(op)){
                return;
            }
            _source.put("server_id",this.conf.server_id);
            _source.put("objName",op.objName);
            _source.put("objHash",Hexa32.toString32(p.objHash));
            _source.put("objType","java");
            _source.put("objFamily","sqlstep");

            _source.put("startTime",this.dateTimeFormatter.format(new Date(p.endTime - p.elapsed).toInstant()));
            _source.put("endTime",this.dateTimeFormatter.format(new Date(p.endTime).toInstant()));

            _source.put("serviceName",this.getString(helper.getServiceString(p.service)));
            _source.put("threadName",this.getString(helper.getHashMsgString(p.threadNameHash)));

            _source.put("txId",Hexa32.toString32(p.txid));


            _source.put("elapsed",p.elapsed);
            _source.put("error",this.getString(helper.getErrorString(p.error)));
            _source.put("sqlCount",p.sqlCount);
            _source.put("sqlTime",p.sqlTime);

            _source.put("userAgent",this.getString(helper.getUserAgentString(p.userAgent)));
            _source.put("referrer",this.getString(helper.getRefererString(p.referer)));

            _source.put("apiCallCount",p.apicallCount);
            _source.put("apiCallTime",p.apicallTime);
            log.info("{}",_source);

    }
    private String getString(String value){
        return StringUtil.nullToEmpty(value);
    }

}
