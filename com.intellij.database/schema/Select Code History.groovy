import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import com.intellij.database.model.DasRoutine
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiFileFactory
import com.intellij.sql.dialects.mssql.MsDialect
import com.intellij.openapi.application.ApplicationManager

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

SELECTION.each { openScript(it) }

def openScript(table) {
	def db = DasUtil.getCatalog(table)
	def schema = DasUtil.getNamespace(table).name
	def name = DasUtil.getName(table)
	def sql = "SELECT *\n" +
	          "FROM HistoryCode.dbo.EventsLog WITH (NOLOCK)\n" +
	          "WHERE DatabaseName = '" + db + "'\n" +
	          "AND SchemaName = '" + schema + "'\n" +
	          "AND ObjectName = '" + name + "'\n" +
	          "ORDER BY 1 DESC;"

    ApplicationManager.getApplication().invokeLater {
        def psiFile = PsiFileFactory.getInstance(PROJECT).createFileFromText(MsDialect.INSTANCE, sql)
        psiFile.virtualFile.rename(null, "generated_" + UUID.randomUUID().toString().substring(0, 8) + ".sql")
        FileEditorManager.getInstance(PROJECT).openFile(psiFile.virtualFile, true)
    }
}
