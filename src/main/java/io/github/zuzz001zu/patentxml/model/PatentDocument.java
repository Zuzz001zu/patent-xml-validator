package io.github.zuzz001zu.patentxml.model;

import java.util.List;

/** The bibliographic data of one international publication, as read from the XML. */
public record PatentDocument(
        Field publicationLanguage,
        Field filingLanguage,
        DocumentId publication,
        DocumentId application,
        List<PriorityClaim> priorityClaims,
        List<Classification> classifications,
        List<Party> applicants,
        List<Party> inventors,
        List<Title> titles) {

    public PatentDocument {
        priorityClaims = List.copyOf(priorityClaims);
        classifications = List.copyOf(classifications);
        applicants = List.copyOf(applicants);
        inventors = List.copyOf(inventors);
        titles = List.copyOf(titles);
    }
}
