package com.easystyleshop.massengerapp

import org.junit.Test
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

class ExampleUnitTest {
    @Test
    fun testReadExcelColumns() {
        val file = File("src/main/res/raw/email.xlsx")
        try {
            file.inputStream().use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                var count = 0
                for (row in sheet) {
                    print("Row ${row.rowNum}: ")
                    for (c in 0 until row.lastCellNum) {
                        val cell = row.getCell(c)
                        print("Col $c: '$cell' | ")
                    }
                    println()
                    count++
                    if (count >= 10) break
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
