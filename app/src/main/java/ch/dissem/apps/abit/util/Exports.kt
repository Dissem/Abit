package ch.dissem.apps.abit.util

import android.content.Context
import android.net.Uri
import ch.dissem.apps.abit.service.Singleton
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.exports.ContactExport
import ch.dissem.bitmessage.exports.MessageExport
import ch.dissem.bitmessage.utils.UnixTime
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Parser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Helper object for data export and import.
 */
object Exports {

    fun exportData(target: File, ctx: Context): File {
        val temp = if (target.isDirectory) {
            File(target, "export-${UnixTime.now}.zip")
        } else {
            target
        }
        ZipOutputStream(FileOutputStream(temp)).use { zip ->
            zip.putNextEntry(ZipEntry("contacts.json"))
            val addressRepo = Singleton.getAddressRepository(ctx)
            val exportContacts = ContactExport.exportContacts(addressRepo.getContacts())
            zip.write(
                exportContacts.toJsonString(true).toByteArray()
            )
            zip.closeEntry()

            val labelRepo = Singleton.getLabelRepository(ctx)
            zip.putNextEntry(ZipEntry("labels.json"))
            val exportLabels = MessageExport.exportLabels(labelRepo.getLabels())
            zip.write(
                exportLabels.toJsonString(true).toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("messages.json"))
            val messageRepo = Singleton.getMessageRepository(ctx)
            val exportMessages = MessageExport.exportMessages(messageRepo.getAllMessages())
            zip.write(
                exportMessages.toJsonString(true).toByteArray()
            )
            zip.closeEntry()
        }

        return temp
    }

    fun importData(zipFile: Uri, ctx: Context) {
        val bmc = Singleton.getBitmessageContext(ctx)
        val labels = mutableMapOf<String, Label>()

        processEntry(ctx, zipFile, "contacts.json") { json ->
            ContactExport.importContacts(json).forEach { contact ->
                bmc.addresses.save(contact)
            }
        }
        bmc.labels.getLabels().forEach { label ->
            labels[label.toString()] = label
        }
        processEntry(ctx, zipFile, "labels.json") { json ->
            MessageExport.importLabels(json).forEach { label ->
                if (!labels.contains(label.toString())) {
                    bmc.labels.save(label)
                    labels[label.toString()] = label
                }
            }
        }
        processEntry(ctx, zipFile, "messages.json") { json ->
            MessageExport.importMessages(json, labels).forEach { message ->
                bmc.messages.save(message)
            }
        }
    }

    private fun processEntry(ctx: Context, zipFile: Uri, entry: String, processor: (JsonArray<*>) -> Unit) =
        ZipInputStream(ctx.contentResolver.openInputStream(zipFile)).use { zip ->
            var nextEntry = zip.nextEntry
            while (nextEntry != null) {
                if (nextEntry.name == entry) {
                    processor(Parser().parse(zip) as JsonArray<*>)
                }
                nextEntry = zip.nextEntry
            }
        }

}
