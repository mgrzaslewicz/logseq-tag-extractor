package com.mg.logseq.extractor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LogseqTagExtractorTest {
    @Test
    fun shouldExtractPathToTagAndItsContent() {
        // given
        val markdown = """
            # Title 0
              - subtitle 0.1
              - subtitle 0.2
                - subcontent 0.2.1 [[tag 1]]
                  - subcontent 0.2.1.1
                - subcontent 0.2.2 #tag-2
            # Title 1

            ## Title 1.1
            - First item
              - Subitem 1.1
                - Subitem 1.1.1
              - Subitem 1.2
              - Subitem 1.3 and #some-tag
                - Content
            - Second item
            ::property:: value

            ## Title 1.2
            - Logseq specific [[link]]
              - Nested item referencing [[other link]]
              """.trimIndent()
        val extractor = LogseqTagExtractor(markdown)
        // when
        val parsedLines = extractor.extract("tag 1")
        // then
        assertThat(parsedLines).containsExactly(
            "# Title 0",
            "  - subtitle 0.2",
            "    - subcontent 0.2.1 [[tag 1]]",
            "      - subcontent 0.2.1.1",

        )
    }

}
