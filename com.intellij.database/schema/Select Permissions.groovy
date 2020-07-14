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

	def sql = "USE " + db + "\n" +
	          "GO\n" +
	          "SELECT database_permissions.state_desc COLLATE DATABASE_DEFAULT\n" +
	          "           + ' ' + database_permissions.permission_name COLLATE DATABASE_DEFAULT\n" +
	          "           + ' ON ' + QUOTENAME(schemas.name) + '.' + QUOTENAME(objects.name)\n" +
	          "           + ' TO ' + database_principals.name\n" +
	          "FROM sys.schemas\n" +
	          "INNER JOIN sys.objects ON objects.schema_id = schemas.schema_id\n" +
	          "INNER JOIN sys.database_permissions ON database_permissions.class_desc = 'OBJECT_OR_COLUMN' AND database_permissions.major_id = objects.object_id\n" +
	          "INNER JOIN sys.database_principals ON database_principals.principal_id = database_permissions.grantee_principal_id\n" +
	          "WHERE schemas.name = '" + schema + "' AND objects.name = '" + name + "'\n"

    ApplicationManager.getApplication().invokeLater {
        def psiFile = PsiFileFactory.getInstance(PROJECT).createFileFromText(MsDialect.INSTANCE, sql)
        psiFile.virtualFile.rename(null, "generated_" + UUID.randomUUID().toString().substring(0, 8) + ".sql")
        FileEditorManager.getInstance(PROJECT).openFile(psiFile.virtualFile, true)
    }
}