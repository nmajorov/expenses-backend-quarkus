package biz.majorov.expenses



import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import io.restassured.parsing.Parser
import org.apache.camel.CamelContext
import javax.inject.Inject
import javax.ws.rs.core.Response

/**
 * test Expenses API
 */
@QuarkusTest
class ExpenseApiTest : OAuthTest() {


    @Test
    fun `test get all expenses`() {
        println("\n\n **** ${object {}.javaClass.enclosingMethod.name} ***** \n ")
        println("\n use token:$TOKEN")
        val result = given().contentType("application/json").queryParam("reportid",1)
                .header("Authorization","Bearer " + TOKEN)
                .`when`().get("/expenses").`as` (mutableListOf<HashMap<String?, String?>>()::class.java)

        assertFalse(result.isEmpty())

    }

    @Test
    fun `test select one expense item`() {
        println("\n\n **** ${object {}.javaClass.enclosingMethod.name} ***** \n ")
        println("\n use token:$TOKEN")
        println("\n first get all expenses")
        val response = given().contentType("application/json")
                .header("Authorization", "Bearer $TOKEN")
                .body(1)
                .`when`()
                .get("/expenses")
                .`as`(mutableListOf<HashMap<String?, String?>>()::class.java)

        assertFalse(response.isEmpty())

        val expenseFromRequest = Expense()
        expenseFromRequest.id = response.last()["id"] as Int
        expenseFromRequest.description = response.last()["description"] as String

        expenseFromRequest.amount = response.last()["amount"] as Double
        expenseFromRequest.createdAT = LocalDate.parse(response.last()["createdAT"] as String,DateTimeFormatter.ISO_DATE)

        val response2 = given()
                .header("Authorization", "Bearer $TOKEN")
                .get("/expenses/" + expenseFromRequest.id)
                .`as` (hashMapOf<Any?, Any?>()::class.java)
        println("get response:${response}")
        val id = response2["id"]
        println("got id from restful service call:$id")
        assertEquals(id, expenseFromRequest.id)
        assertEquals(response2["amount"], expenseFromRequest.amount)

    }

    @Test
    fun testCreateExpense() {
        println("${object {}.javaClass.enclosingMethod.name} ")
        RestAssured.defaultParser = Parser.JSON;

        val expense = Expense()
        expense.amount=10.12
        expense.description ="train ticket"
        expense.createdAT= LocalDate.now()


        println("step1 post item  $expense")
        given().contentType("application/json").queryParam("reportid",1)
                .header("Authorization", "Bearer $TOKEN")
                .body(expense)
                .`when`().post("/expenses").then().statusCode(200)

    }

    @Test
    fun testUpdateExpense(){
        println("${object {}.javaClass.enclosingMethod.name} ")
        val expense = Expense()
        expense.amount=80.12
        expense.description ="train ticket"
        expense.createdAT= LocalDate.now()

        println("step1 create item  $expense")
        given().contentType("application/json").queryParam("reportid",1).body(expense)
                .header("Authorization","Bearer " + TOKEN)
                .`when`().post("/expenses").then().statusCode(200)

        println("get all expenses")
        val response = given()
                .contentType("application/json")
                .header("Authorization","Bearer " + TOKEN)
                .queryParam("reportid",1)
                .get("/expenses").`as`(mutableListOf<HashMap<String?, String?>>()::class.java)
        val allExpenseSizeBeforDelete = response.size
        println("get ${allExpenseSizeBeforDelete} expenses")
        println("get last expense from received list")
        val expenseFromRequest = Expense()
        expenseFromRequest.id = response.last()["id"] as Int
        expenseFromRequest.description = response.last()["description"] as String
        expenseFromRequest.amount = response.last()["amount"] as Double

        val dt = LocalDate.parse(response.last()["createdAT"] as String,DateTimeFormatter.ISO_DATE)
        expenseFromRequest.createdAT = dt

        //be sure you take the same expense
        assertEquals(expenseFromRequest.amount , expense.amount)

        println("change amount")

        expenseFromRequest.amount=45.99
        given().contentType("application/json").body(expenseFromRequest)
                .header("Authorization","Bearer " + TOKEN)
                .`when`().put("/expenses").then().statusCode(200)
        println ("get updated expense and check if it has a different amount")
        val responseStep2 = given().get("/expenses/" + expenseFromRequest.id).`as` (hashMapOf<Any?, Any?>()::class.java)

        assertEquals(expenseFromRequest.amount,responseStep2["amount"] as Double)


    }

    @Test
    fun `test delete expense item`(){
        println("${object {}.javaClass.enclosingMethod.name} ")

        val expense = Expense()
        expense.amount=110.12
        expense.description ="train ticket"
        expense.createdAT= LocalDate.now()

        println("step1 create item  $expense")
        given().contentType("application/json").body(expense)
                .header("Authorization", "Bearer $TOKEN")
                .`when`().post("/expenses").then().statusCode(200)

        println("get all expenses")
        var response = given().contentType("application/json").body(1)
                .header("Authorization","Bearer " + TOKEN)
                .get("/expenses")
                .`as`(mutableListOf<HashMap<String?, String?>>()::class.java)
        val allExpenseSizeBeforDelete = response.size
        println("get $allExpenseSizeBeforDelete expenses")
        println("get last expense from received list")
        val expenseFromRequest = Expense()
        expenseFromRequest.id = response.last()["id"] as Int
        expenseFromRequest.description = response.last()["description"] as String
        expenseFromRequest.amount = response.last()["amount"] as Double

        val dt = LocalDate.parse(response.last()["createdAT"] as String,DateTimeFormatter.ISO_DATE)
        expenseFromRequest.createdAT = dt

        //be sure last expenses is inserted one
        assertEquals(expense.amount,expenseFromRequest.amount)

        println("step 3 delete expense with id ${expenseFromRequest.id}" )
        given().contentType("application/json")
                .header("Authorization","Bearer " + TOKEN)
                .`when`().delete("/expenses/" + expenseFromRequest.id)
                .then().statusCode(200)

        response = given().contentType("application/json")
                .body(1)
                .header("Authorization", "Bearer $TOKEN")
                .get("/expenses")
                .`as`(mutableListOf<HashMap<String?, String?>>()::class.java)

        val allExpenseSizeAfterDelete = response.size

        assertTrue(allExpenseSizeAfterDelete < allExpenseSizeBeforDelete)
        assertEquals(allExpenseSizeAfterDelete,(allExpenseSizeBeforDelete -1))

    }

}