package de.zbw.business.lori.server.type

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.business.lori.server.utils.SearchExpressionResolution
import io.ktor.http.cio.ParserException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SearchExpressionTest {

    @DataProvider(name = DATA_FOR_PARSING_SEARCH_QUERY)
    fun createDataForParsing() =
        arrayOf(
            arrayOf(
                "tit:'foo'",
                "Variable(searchPair=TITLE:foo)",
                false,
                "Single Search Pair",
            ),
            arrayOf(
                "!tit:'foo'",
                "Not(body=Variable(searchPair=TITLE:foo))",
                false,
                "One negation",
            ),
            arrayOf(
                "tit:'foo'|zdb:'123'",
                "Or(left=Variable(searchPair=TITLE:foo), right=Variable(searchPair=ZDB_ID:123))",
                false,
                "Or search pairs no whitespace",
            ),
            arrayOf(
                "tit:'foo' | zdb:'123'",
                "Or(left=Variable(searchPair=TITLE:foo), right=Variable(searchPair=ZDB_ID:123))",
                false,
                "Or search pairs",
            ),
            arrayOf(
                "tit:'foo' & zdb:'123'",
                "And(left=Variable(searchPair=TITLE:foo), right=Variable(searchPair=ZDB_ID:123))",
                false,
                "And search pairs"
            ),
            arrayOf(
                "(tit:'foo' & zdb:'123') | hdl:'123'",
                "Or(left=And(left=Variable(searchPair=TITLE:foo), right=Variable(searchPair=ZDB_ID:123)), right=Variable(searchPair=HDL:123))",
                false,
                "Or, And, parentheses"
            ),
            // TODO(CB): Add more test cases, especially error cases
        )

    @Test(dataProvider = DATA_FOR_PARSING_SEARCH_QUERY)
    fun testExpressionParser(
        query: String,
        expected: String,
        throwsException: Boolean,
        details: String,
    ) {
        try {
            when (val expr: ParseResult<SearchExpression> = SearchGrammar.tryParseToEnd(query)) {
                is Parsed -> {
                    assertThat(
                        details,
                        expr.value.toString(),
                        `is`(expected),
                    )
                    assertTrue(!throwsException)
                }

                is ErrorResult -> Assert.fail()
            }
        } catch (pe: ParserException) {
            assertTrue(throwsException)
        }
    }

    @DataProvider(name = DATA_FOR_RESOLVE_SEARCH_EXPRESSION)
    fun createDataForResolveSearchExpression() =
        arrayOf(
            arrayOf(
                "(tit:'foo' & zdb:'123') | hdl:'123'",
                "((ts_title @@ to_tsquery(?)) AND ts_zdb_id @@ to_tsquery(?)) OR ts_hdl @@ to_tsquery(?)",
            ),
        )

    @Test(dataProvider = DATA_FOR_RESOLVE_SEARCH_EXPRESSION)
    fun resolveSearchExpression(
        query: String,
        expected: String,
    ) {
        assertThat(
            SearchExpressionResolution.resolveSearchExpression(SearchGrammar.parseToEnd(query)),
            `is`(expected),
        )
    }

    @DataProvider(name = DATA_FOR_RESOLVE_SEARCH_EXPRESSION_COALESCE)
    fun createDataForResolveSearchExpressionCoalesce() =
        arrayOf(
            arrayOf(
                "(tit:'foo' & zdb:'123') | hdl:'123'",
                "(coalesce(ts_rank_cd(ts_title, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_hdl, to_tsquery(?)),1))/3 as score",
            ),
        )

    @Test(dataProvider = DATA_FOR_RESOLVE_SEARCH_EXPRESSION_COALESCE)
    fun resolveSearchExpressionCoalesce(
        query: String,
        expected: String,
    ) {
        assertThat(
            SearchExpressionResolution.resolveSearchExpressionCoalesce(SearchGrammar.parseToEnd(query)),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PARSING_SEARCH_QUERY = "DATA_FOR_PARSING_SEARCH_QUERY"
        const val DATA_FOR_RESOLVE_SEARCH_EXPRESSION = "DATA_FOR_RESOLVE_SEARCH_EXPRESSION"
        const val DATA_FOR_RESOLVE_SEARCH_EXPRESSION_COALESCE = "DATA_FOR_RESOLVE_SEARCH_EXPRESSION_COALESCE"
    }
}
