package zerobase.dividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import zerobase.dividend.exception.impl.AlreadyExistCompanyException;
import zerobase.dividend.exception.impl.NoCompanyException;
import zerobase.dividend.model.Company;
import zerobase.dividend.model.ScrapedResult;
import zerobase.dividend.persist.CompanyRepository;
import zerobase.dividend.persist.DividendRepository;
import zerobase.dividend.persist.entity.CompanyEntity;
import zerobase.dividend.persist.entity.DividendEntity;
import zerobase.dividend.scrapper.Scraper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final Scraper yahooFinanceScraper;

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if(exists) {
            log.warn("company already exists -> " + ticker);
            throw new AlreadyExistCompanyException();
        }
        log.info("save company -> " + ticker);
        return this.storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {

        log.info("start scrap company by ticker -> " + ticker);
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if(ObjectUtils.isEmpty(company)) {
            log.warn("scrap failed -> " + ticker);
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }
        log.info("success scrap company by ticker -> " + company.getName());

        log.info("start scrap dividend info -> " + company.getName());
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        log.info("save company info -> " + company.getName());
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        log.info("save dividend info -> " + company.getName());
        this.dividendRepository.saveAll(dividendEntities);

        return company;
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream().limit(10).collect(Collectors.toList());
    }

    public void deleteAutocompleteKeyword(String keyword) {
        log.info("delete autocomplete keyword -> " + keyword);
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        CompanyEntity company =  this.companyRepository.findByTicker(ticker)
                .orElseThrow(NoCompanyException::new);

        log.info("delete dividend info -> " + company.getName());
        this.dividendRepository.deleteAllByCompanyId(company.getId());
        log.info("delete company info -> " + company.getName());
        this.companyRepository.delete(company);
        this.deleteAutocompleteKeyword(company.getName());

        return company.getName();
    }
}
