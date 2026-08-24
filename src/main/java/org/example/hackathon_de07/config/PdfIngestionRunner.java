package org.example.hackathon_de07.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfIngestionRunner implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) throws Exception {
        boolean tableExists = false;
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'vector_store'", Integer.class);
            if (tableCount != null && tableCount > 0) {
                tableExists = true;
                Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
                if (count != null && count > 0) {
                    log.info("Dữ liệu vector đã tồn tại ({} bản ghi). Bỏ qua bước nạp PDF.", count);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi kiểm tra bảng vector_store: {}", e.getMessage());
        }

        log.info("Bắt đầu đọc và nạp file De07_FoodHub_ThongTin.pdf vào Vector Store...");
        try {
            Resource pdfResource = resourceLoader.getResource("classpath:De07_FoodHub_ThongTin.pdf");
            if (!pdfResource.exists()) {
                log.error("Không tìm thấy file PDF tại classpath:De07_FoodHub_ThongTin.pdf");
                return;
            }

            TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
            List<Document> documents = documentReader.get();

            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);

            if (!tableExists) {
                Thread.sleep(2000);
            }
            
            vectorStore.add(splitDocuments);
            log.info("Nạp thành công {} chunks vào Vector Store.", splitDocuments.size());
        } catch (Exception e) {
            log.error("Lỗi khi nạp tài liệu: ", e);
        }
    }
}
