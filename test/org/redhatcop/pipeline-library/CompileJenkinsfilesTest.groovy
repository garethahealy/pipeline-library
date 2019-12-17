import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

class CompileJenkinsfilesTest extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        baseScriptRoot = ''
        scriptRoots = 'src/test'
        scriptExtension = ''
        super.setUp()
    }

    @Test
    void doJenkinfilesCompile() throws Exception {
        def script = loadScript("Jenkinsfile-applier")
        script.execute()
        printCallStack()
    }
}
