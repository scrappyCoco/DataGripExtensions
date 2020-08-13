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
	def db     = DasUtil.getCatalog(table)
	def schema = DasUtil.getNamespace(table).name
	def name   = DasUtil.getName(table)
	
	def sql    = "SELECT IndedxName       = indexes.name,\n" +
				 "	   AvgFragmentation = stat.avg_fragmentation_in_percent,\n" +
				 "	   AvgPageUsage     = stat.avg_page_space_used_in_percent,\n" +
				 "	   UnitType         = stat.alloc_unit_type_desc,\n" +
				 "       IndexLevel       = stat.index_level\n" +
				 "FROM " + db + ".sys.schemas\n" +
				 "INNER JOIN " + db + ".sys.objects ON schemas.schema_id = objects.schema_id\n" +
				 "INNER JOIN " + db + ".sys.indexes ON indexes.object_id = objects.object_id\n" +
				 "OUTER APPLY " + db + ".sys.dm_db_index_physical_stats(\n" +
				 "    DB_ID(N'" + db + "'),\n" +
				 "    objects.object_id,\n" +
				 "    indexes.index_id,\n" +
				 "    NULL,\n" +
				 "    NULL\n" +
				 ") AS stat\n" +
				 "WHERE objects.name = '" + name + "' AND schemas.name = '" + schema + "'\n"

    ApplicationManager.getApplication().invokeLater {
        def psiFile = PsiFileFactory.getInstance(PROJECT).createFileFromText(MsDialect.INSTANCE, sql)
        psiFile.virtualFile.rename(null, "generated_" + UUID.randomUUID().toString().substring(0, 8) + ".sql")
        FileEditorManager.getInstance(PROJECT).openFile(psiFile.virtualFile, true)
    }
}