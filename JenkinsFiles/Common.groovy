import java.net.URLEncoder

def cmd(String command) {
    log(command)

    // при запуске Jenkins не в режиме UTF-8 нужно написать chcp 1251 вместо chcp 65001

    if (isUnix()) {
        sh "${command}"
    } else {
        bat """chcp 65001 > nul
            ${command}"""
    }
}

def log(String text) {
    if (env.debug == 'true') {
        echo text
    }
}

def cmdReturnStdout(String command) {
    log(command)

    def output = ''

    if (isUnix()) {
        output = sh script: "${command}",
                        returnStdout: true
    } else {
        output = bat script:
                    """chcp 65001 > null
                    ${command}""",
                    returnStdout: true
    }

    def outputs = output.trim().split(command)

    output = outputs[outputs.length - 1].trim()

    log("Результат cmdReturnStdout: ${output}")

    return output
}

def runner_cli(String command, String commandArgs) {
    cmd("runner ${command} ${commandArgs} --debuglogfile=${logDir}/runner.log --debuglog")
}

def readJSON_with_UTF8BOM(String pathToJSON) {
    String textJSON = readFile pathToJSON

    textJSON = deBOM(textJSON)

    log(textJSON)

    def valuesJSON = readJSON text: textJSON, returnPojo: true

    return valuesJSON
}

def initParam(value, default_value, String name_value) {
    def result_value

    if (value == null || value == 'null' || value.isEmpty()) {
        result_value = default_value
    }
    else {
        result_value = value
    }

    log("${name_value}: ${result_value}")

    return result_value
}

def CommandDebug() {
    def commandDebug = ''

    if (Get_Coverage) {
        commandDebug = "--additional \"/DEBUG -http -attach /debuggerURL ${debuggerURL}\""
    }

    return commandDebug
}

def SafetyDeleteDir() {
    int ntry = 10
    int toSleep = 1
    boolean deleted = false
    while (ntry != 0) {
        try {
            deleteDir()
            ntry = 0
            deleted = true
        }
        catch (e) {
            echo "Не удалось очистить каталог: ${e}"
            ntry--
            sleep(toSleep)
            toSleep = toSleep * 2
        }
    }
    if (!deleted){
        // Если вышли из цикла и так и не удалили, то делаем еще одну попытку без глушения исключения
        deleteDir()
    }
}

String deBOM(String s) {
    if (s == null) {
        return null
    } else if (s.length() == 0) {
        return s
    } else if (s[0] == '\uFEFF') {
        return s.drop(1)
    } else {
        return s
    }
}

def concatStrings(ArrayList ss, boolean addSpaces = false) {
    resultStr = ''

    ss.each { str ->
        resultStr += str
        if (addSpaces) {
            resultStr += ' '
        }
    }

    return resultStr
}

@NonCPS
String encode(String url) {
    return URLEncoder.encode(url, 'UTF-8')
        .replaceAll('\\+', '%20')
        .replaceAll('\\%21', '!')
        .replaceAll('\\%27', "'")
        .replaceAll('\\%28', '(')
        .replaceAll('\\%29', ')')
        .replaceAll('\\%7E', '~')
        .replaceAll('\\%2C', ',')
        .replaceAll('\\%2F\\%2F', '/(empty)/')
        .replaceAll('\\%2F', '/')
}

def deleteFileIfExists(String filePath) {
    if ( fileExists("${filePath}") ) { cmd "rm -f ${filePath}" }
    if ( fileExists("${filePath}") ) { error "Failed to delete file ${filePath}" }
}

String xml_From_EDT(String projectPath, String build_dir) {
    dir("${build_dir}/XML") {
        deleteDir()
    }
    dir("${build_dir}/WP") {
        deleteDir()
    }

    def ringOpts = 'SET RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru'

    def edtCommand = "ring edt workspace export --workspace-location ${build_dir}/WP --project ${projectPath} --configuration-files ${build_dir}/XML"

    cmd("""${ringOpts}
           ${edtCommand}""")

    return "${build_dir}/XML"
}

// Для возможности использования как модуль

log('Подключен общий модуль Common')

return this
