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
	def sql = "SELECT [Index]        = indexes.name,\n" +
	          "       AllocationType = allocation_units.type_desc,\n" +
	          "       Gb             = allocation_units.total_pages * 8 / 1024.0 / 1024.0,\n" +
	          "       Rows           = partitions.rows\n" +
	          "FROM [" + db + "].sys.objects\n" +
	          "INNER JOIN [" + db + "].sys.schemas ON schemas.schema_id = objects.schema_id\n" +
	          "INNER JOIN [" + db + "].sys.partitions ON objects.object_id = partitions.object_id\n" +
	          "INNER JOIN [" + db + "].sys.allocation_units ON partitions.partition_id = allocation_units.container_id\n" +
	          "INNER JOIN [" + db + "].sys.indexes ON indexes.object_id = partitions.object_id AND indexes.index_id = partitions.index_id\n" +
	          "WHERE objects.name = '" + name + "' AND schemas.name = '" + schema + "';"

    ApplicationManager.getApplication().invokeLater {
        def psiFile = PsiFileFactory.getInstance(PROJECT).createFileFromText(MsDialect.INSTANCE, sql)
        psiFile.virtualFile.rename(null, "generated_" + UUID.randomUUID().toString().substring(0, 8) + ".sql")
        FileEditorManager.getInstance(PROJECT).openFile(psiFile.virtualFile, true)
    }
}