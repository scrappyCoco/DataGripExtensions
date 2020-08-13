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
	
	def sql    = "USE [" + db + "]\nGO\n" +
				 "SELECT stats.name,\n" +
				 "	   stats.stats_id,\n" +
				 "	   Columns            = (\n" +
				 "								SELECT s = STRING_AGG(columns.name, ',')\n" +
				 "								FROM sys.stats_columns\n" +
				 "								INNER JOIN sys.columns ON columns.object_id = stats_columns.object_id AND\n" +
				 "														  columns.column_id = stats_columns.column_id\n" +
				 "								WHERE\n" +
				 "									  stats.object_id = stats_columns.object_id\n" +
				 "								  AND stats.stats_id = stats_columns.stats_id\n" +
				 "							),\n" +
				 "	   stats.filter_definition,\n" +
				 "	   stats.no_recompute,\n" +
				 "	   LastStatUpdateDate = dm_db_stats_properties.last_updated,\n" +
				 "	   StatRows           = dm_db_stats_properties.rows,\n" +
				 "	   StatSampledRows    = dm_db_stats_properties.rows_sampled,\n" +
				 "	   StatSteps          = dm_db_stats_properties.steps,\n" +
				 "	   StatModCounter     = dm_db_stats_properties.modification_counter\n" +
				 "FROM sys.schemas\n" +
				 "INNER JOIN sys.objects ON objects.schema_id = schemas.schema_id\n" +
				 "INNER JOIN sys.stats ON stats.object_id = objects.object_id\n" +
				 "OUTER APPLY sys.dm_db_stats_properties(stats.object_id, stats.stats_id)\n" +
				 "WHERE schemas.name = '" + schema + "'\n" +
				 "  AND objects.name = '" + name + "'\n"

    ApplicationManager.getApplication().invokeLater {
        def psiFile = PsiFileFactory.getInstance(PROJECT).createFileFromText(MsDialect.INSTANCE, sql)
        psiFile.virtualFile.rename(null, "generated_" + UUID.randomUUID().toString().substring(0, 8) + ".sql")
        FileEditorManager.getInstance(PROJECT).openFile(psiFile.virtualFile, true)
    }
}