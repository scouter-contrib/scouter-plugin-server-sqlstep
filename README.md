# scouter-plugin-server-sqlstep

                                              
![Korean](https://img.shields.io/badge/language-Korean-blue.svg)
- 본프로젝트는 스카우터 서버 플로그인으로써 XLOG 정보에서 Service 와 SQL 정보만 추출 하여 히스토리 형태의 파일 형태로 남기는 플러그인 이다.  
   

### configuration (스카우터 서버 설치 경로 하위의 conf/scouter.conf)
#### 기본 설정
* **ext_plugin_sqlstep_enabled** : 본 plugin 사용 여부 (default : true)
* **ext_plugin_sqlstep_index** : SQL로깅 파일 index의 Prefix 명 (default : sqltstep-xlog)
* **ext_plugin_sqlstep_duration_day** :  SQL 로깅 index 저장 기간 (default : 3) 
* **ext_plugin_sqlstep_root_dir** : Log를 저장할 Root 디렉토리명 (default : 스카우터 설치 홈/ext_plugin_sqlstep)
* **ext_plugin_sqlstep_rotate_dir** : Log를 Rotate 할 디렉토리명 (default : 스카우터 설치 홈/ext_plugin_sqlstep/rotate) 
* **ext_plugin_sqlstep_debug_enabled** : SQL Step 디버깅(default: false) 
         
### dependencies
Refer to [pom.xml](./pom.xml)

### Build environment 
 - Java 1.8.x 이상 
 - Maven 3.x 이상 

### Build
 - mvn clean package

 
