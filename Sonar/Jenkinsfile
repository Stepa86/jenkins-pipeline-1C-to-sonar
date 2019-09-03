
def BIN_CATALOG = ''
def ACC_PROPERTIES = ''
def ACC_BASE = ''
def ACC_USER = ''
def BSL_LS_PROPERTIES = ''
def CURRENT_CATALOG = ''
def TEMP_CATALOG = ''
def PROJECT_NAME_EDT = ''
def PROJECT_KEY
def EDT_VALIDATION_RESULT = ''
def GENERIC_ISSUE_JSON = ''
def SRC = ''
def PROJECT_URL = ''

pipeline {

    parameters {
        string(defaultValue: "${env.PROJECT_NAME}", description: '* Имя проекта. Одинаковое для EDT, проекта в АПК и в сонаре. Обычно совпадает с именем конфигурации.', name: 'PROJECT_NAME')
        string(defaultValue: "${env.git_repo_url}", description: '* URL к гит-репозиторию, который необходимо проверить.', name: 'git_repo_url')
        string(defaultValue: "${env.git_repo_branch}", description: 'Ветка репозитория, которую необходимо проверить. По умолчанию master', name: 'git_repo_branch')
        string(defaultValue: "${env.sonar_catalog}", description: 'Каталог сонара, в котором лежит все, что нужно. По умолчанию C:/Sonar/', name: 'sonar_catalog')
        string(defaultValue: "${env.PROPERTIES_CATALOG}", description: 'Каталог с настройками acc.properties, bsl-language-server.conf и sonar-project.properties. По умолчанию ./Sonar', name: 'PROPERTIES_CATALOG')
        booleanParam(defaultValue: env.ACC_check== null ? true : env.ACC_check, description: 'Выполнять ли проверку АПК. Если нет, то будут получены существующие результаты. По умолчанию: true', name: 'ACC_check')
        booleanParam(defaultValue: env.ACC_recreateProject== null ? false : env.ACC_recreateProject, description: 'Пересоздать проект в АПК. Все данные о проекте будут собраны заново. По умолчанию: false', name: 'ACC_recreateProject')
        string(defaultValue: "${env.STEBI_SETTINGS}", description: 'Файл настроек для переопределения замечаний. Для файла из репо проекта должен начинатся с папки Repo, например .Repo/Sonar/settings.json. По умолчанию ./Sonar/settings.json', name: 'STEBI_SETTINGS')
        string(defaultValue: "${env.jenkinsAgent}", description: 'Нода дженкинса, на которой запускать пайплайн. По умолчанию master', name: 'jenkinsAgent')
        string(defaultValue: "${env.EDT_VERSION}", description: 'Используемая версия EDT. По умолчанию 1.13.0', name: 'EDT_VERSION')
        string(defaultValue: "${env.perf_catalog}", description: 'Путь к каталогу с замерами производительности, на основе которых будет рассчитано покрытие. Если пусто - покрытие не считается.', name: 'perf_catalog')
        string(defaultValue: "${env.git_credentials_Id}", description: 'ID Credentials для получения изменений из гит-репозитория', name: 'git_credentials_Id')
        string(defaultValue: "${env.rocket_channel}", description: 'Канал в рокет-чате для отправки уведомлений', name: 'rocket_channel')
    }
    agent {
        label "${(env.jenkinsAgent == null || env.jenkinsAgent == 'null') ? "master" : env.jenkinsAgent}"
    }
    options {
        timeout(time: 8, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage("Инициализация переменных") {
            steps {
                timestamps {
                    script {

                        rocket_channel = rocket_channel == null || rocket_channel == 'null' ? '' : rocket_channel

                        if (!rocket_channel.isEmpty() ) {
                            rocketSend channel: rocket_channel, message: "Sonar check started: [${env.JOB_NAME} ${env.BUILD_NUMBER}](${env.JOB_URL})", rawMessage: true
                        }
                        // Инициализация параметров значениями по умолчанию
                        sonar_catalog = sonar_catalog.isEmpty() ? "C:/Sonar/" : sonar_catalog
                        PROPERTIES_CATALOG = PROPERTIES_CATALOG.isEmpty() ? "./Sonar" : PROPERTIES_CATALOG
                        
                        EDT_VERSION = EDT_VERSION.isEmpty() ? '1.13.0' : EDT_VERSION
                        STEBI_SETTINGS = STEBI_SETTINGS.isEmpty() ? './Sonar/settings.json' : STEBI_SETTINGS
                        git_repo_branch = git_repo_branch.isEmpty() ? 'master' : git_repo_branch

                        perf_catalog = perf_catalog == null || perf_catalog == 'null' ? '' : perf_catalog
                        
                        BIN_CATALOG = "${sonar_catalog}/bin/"
                        ACC_BASE = "${sonar_catalog}/ACC/"
                        ACC_USER = 'Admin'
                        SRC = "./${PROJECT_NAME}/src"

                        // Подготовка переменных по переданным параметрам
                        // Настройки инструментов
                        ACC_PROPERTIES = "./Repo/${PROPERTIES_CATALOG}/acc.properties"
                        if (fileExists(ACC_PROPERTIES)) {
                            echo "file exists: ${ACC_PROPERTIES}"
                        } else {
                            echo "file does not exist: ${ACC_PROPERTIES}"
                            ACC_PROPERTIES = "./Sonar/acc.properties"
                        }
                        BSL_LS_PROPERTIES = "./Repo/${PROPERTIES_CATALOG}/bsl-language-server.conf"
                        if (fileExists(BSL_LS_PROPERTIES)) {
                            echo "file exists: ${BSL_LS_PROPERTIES}"
                        } else {
                            echo "file does not exist: ${BSL_LS_PROPERTIES}"
                            BSL_LS_PROPERTIES = "./Sonar/bsl-language-server.conf"
                        }
                        
                        CURRENT_CATALOG = pwd()
                        TEMP_CATALOG = "${CURRENT_CATALOG}\\sonar_temp"
                        EDT_VALIDATION_RESULT = "${TEMP_CATALOG}\\edt-result.csv"
                        CURRENT_CATALOG = "${CURRENT_CATALOG}\\Repo"

                        // создаем/очищаем временный каталог
                        dir(TEMP_CATALOG) {
                            deleteDir()
                            writeFile file: 'acc.json', text: '{"issues": []}'
                            writeFile file: 'bsl-generic-json.json', text: '{"issues": []}'
                            writeFile file: 'edt.json', text: '{"issues": []}'
                        }
                        PROJECT_NAME_EDT = "${CURRENT_CATALOG}\\${PROJECT_NAME}"
                        if (git_repo_branch == 'master') {
                            PROJECT_KEY = PROJECT_NAME
                        } else {
                            PROJECT_KEY = "${PROJECT_NAME}_${git_repo_branch}"
                        }
                        
                        GENERIC_ISSUE_JSON ="${TEMP_CATALOG}/acc.json,${TEMP_CATALOG}/bsl-generic-json.json,${TEMP_CATALOG}/edt.json"
                    }
                }
            }
        }
        stage('Checkout') {
            steps {
                timestamps {
                    script {
                        dir('Repo') {
                            checkout([$class: 'GitSCM',
                            branches: [[name: "*/${git_repo_branch}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'CheckoutOption', timeout: 60], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: false, timeout: 60]],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: git_credentials_Id, url: git_repo_url]]])
                        }
                    }
                }
            }
        }
        stage('АПК') {
            steps {
                timestamps {
                    script {
                        def cmd_properties = "\"acc.propertiesPaths=${ACC_PROPERTIES};acc.catalog=${CURRENT_CATALOG};acc.sources=${SRC};acc.result=${TEMP_CATALOG}\\acc.json;acc.projectKey=${PROJECT_KEY};acc.check=${ACC_check};acc.recreateProject=${ACC_recreateProject}\""
                        cmd("runner run --ibconnection /F${ACC_BASE} --db-user ${ACC_USER} --command ${cmd_properties} --execute \"${BIN_CATALOG}acc-export.epf\" --ordinaryapp=1")
                    }
                }
            }
        }
        stage('EDT') {
            steps {
                timestamps {
                    script {
                        if (fileExists("${EDT_VALIDATION_RESULT}")) {
                            cmd("@DEL \"${EDT_VALIDATION_RESULT}\"")
                        }
                        cmd("""
                        @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
                        ring edt@${EDT_VERSION} workspace validate --workspace-location \"${TEMP_CATALOG}\" --file \"${EDT_VALIDATION_RESULT}\" --project-list \"${PROJECT_NAME_EDT}\"
                        """)
                    }
                }
            }
        }
        stage('bsl-language-server') {
            steps {
                timestamps {
                    script {
                    cmd("java -Xmx8g -jar ${BIN_CATALOG}bsl-language-server.jar -a -s \"./Repo/${SRC}\" -r generic -c \"${BSL_LS_PROPERTIES}\" -o \"${TEMP_CATALOG}\"")
                    }
                }
            }
        }
        stage('Конвертация результатов EDT') {
            steps {
                timestamps {
                    script {
                    dir('Repo') {
                        cmd("""
                        set SRC=\"${SRC}\"
                        stebi convert -e \"${EDT_VALIDATION_RESULT}\" \"${TEMP_CATALOG}/edt.json\" 
                        """)
                    }
                    }
                }
            }
        }
        stage('Трансформация результатов') {
            steps {
                timestamps {
                    script {
                    cmd("""
                    set GENERIC_ISSUE_SETTINGS_JSON=\"${STEBI_SETTINGS}\"
                    set GENERIC_ISSUE_JSON=${GENERIC_ISSUE_JSON}
                    set SRC=\"./Repo/${SRC}\"

                    stebi transform -r=0
                    """)
                    }
                }
            }
        }
        stage('Получение покрытия') {
            steps {
                timestamps {
                    script {
                     if (!perf_catalog.isEmpty()) {
                        dir('Repo') {
                            cmd("perf-measurements-to-cover c -i=${perf_catalog} -o=\"${TEMP_CATALOG}\\genericCoverage.xml\" -s=\"${SRC}\"")
                        }
                    } else{
                        echo "skip"
                    }
                    }
                }
            }
        }
        stage('Сканер') {
            steps {
                timestamps {
                    script {
                    dir('Repo') {
                    withSonarQubeEnv('Sonar') {
                    def scanner_properties = "-X -Dsonar.projectVersion=%SONAR_PROJECTVERSION% -Dsonar.projectKey=${PROJECT_KEY} -Dsonar.sources=\"${SRC}\" -Dsonar.externalIssuesReportPaths=${GENERIC_ISSUE_JSON} -Dsonar.sourceEncoding=UTF-8 -Dsonar.inclusions=**/*.bsl -Dsonar.bsl.languageserver.enabled=false"
                    if (!perf_catalog.isEmpty()) {
                        scanner_properties = "${scanner_properties} -Dsonar.coverageReportPaths=\"${TEMP_CATALOG}\\genericCoverage.xml\""
                    }
                    def scannerHome = tool 'SonarQube Scanner';
                    cmd("""
                    @set SRC=\"${SRC}\"
                    @echo %SRC%
                    @call stebi g > temp_SONAR_PROJECTVERSION
                    @set /p SONAR_PROJECTVERSION=<temp_SONAR_PROJECTVERSION
                    @DEL temp_SONAR_PROJECTVERSION
                    @echo %SONAR_PROJECTVERSION%
                    @set JAVA_HOME=${sonar_catalog}\\jdk\\
                    @set SONAR_SCANNER_OPTS=-Xmx6g
                    ${scannerHome}\\bin\\sonar-scanner ${scanner_properties} -Dfile.encoding=UTF-8
                    """)
                    PROJECT_URL = "${env.SONAR_HOST_URL}/dashboard?id=${PROJECT_KEY}"
                    }

                    if (!rocket_channel.isEmpty() ) {
                        def qg = waitForQualityGate()
                        rocketSend channel: rocket_channel, message: "Sonar check completed: [${env.JOB_NAME} ${env.BUILD_NUMBER}](${env.JOB_URL}) STATUS: [${qg.status}](${PROJECT_URL})", rawMessage: true
                        }
                    }
                    }
                }
            }
        }
    }
}
def cmd(command) {
    // при запуске Jenkins не в режиме UTF-8 нужно написать chcp 1251 вместо chcp 65001
    if (isUnix()) { sh "${command}" } else { bat "chcp 65001\n${command}" }
}
