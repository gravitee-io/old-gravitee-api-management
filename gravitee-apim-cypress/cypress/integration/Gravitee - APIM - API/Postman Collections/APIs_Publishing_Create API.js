/// <reference types="Cypress" /> 

describe('Postman Collection - APIs-Publishing-Not Published - Anonymous', ()=>{
            
    let accesToken='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZDgwMGQwMC1lZDJmLTQxYzgtODAwZC0wMGVkMmZhMWM4NTIiLCJwZXJtaXNzaW9ucyI6W3siYXV0aG9yaXR5IjoiRU5WSVJPTk1FTlQ6QURNSU4ifSx7ImF1dGhvcml0eSI6Ik9SR0FOSVpBVElPTjpBRE1JTiJ9LHsiYXV0aG9yaXR5IjoiT1JHQU5JWkFUSU9OOkFETUlOIn0seyJhdXRob3JpdHkiOiJPUkdBTklaQVRJT046VVNFUiJ9LHsiYXV0aG9yaXR5IjoiRU5WSVJPTk1FTlQ6VVNFUiJ9LHsiYXV0aG9yaXR5IjoiRU5WSVJPTk1FTlQ6QURNSU4ifV0sImlzcyI6ImdyYXZpdGVlLW1hbmFnZW1lbnQtYXV0aCIsImV4cCI6MTYzODE5MTMxNiwiaWF0IjoxNjM3NTg2NTE2LCJqdGkiOiJiZGY2YTkzMS1iNmNjLTQ0MDEtYjE2OC0wMzBjYjMxYTM0NmIifQ.MRs1zD3WKkvgv5e1I78-HhDsfFWKV6rO0YCce2egENE; XSRF-TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJncmF2aXRlZS1tYW5hZ2VtZW50LWF1dGgiLCJpYXQiOjE2Mzc4MzUwOTQsInRva2VuIjoiMWI2MTlmOGItNzg4MC00ZTAyLTljYjUtMTFhOTY2MGQ4YTVhIn0.SPbEhWO73lXZjO17w8lokF-B31tlV-S84G15eQF_K54'
    let user='YWRtaW46YWRtaW4='
    let apiID='70b81cf5-0210-46c1-b81c-f50210e6c108'
    
        it('Get APIs does not contain created api', ()=>{
            
            cy.request({
            method: 'GET',
                url: 'http://localhost:8083/portal/environments/DEFAULT/apis',
                headers: {
                    'authorization': "Basic YWRtaW46YWRtaW4=",
                    'Cookie': "XSRF-TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJncmF2aXRlZS1tYW5hZ2VtZW50LWF1dGgiLCJpYXQiOjE2Mzg0MjU2NDgsInRva2VuIjoiYzY0MjVkZmItNGM0Zi00MTYzLTlmOGItZTU5MTdiNWI3OTlkIn0.mA4pLn2E3t-aLOBWj6xHxfBByzwzJ7eO4nTfnyu1EqM"
              },    
            }).then((res)=>{
            cy.log(res)
            expect (res.status).to.eq(200)
            expect(res.body.metadata.pagination.total).to.eq(1)
            expect(res.body.metadata.data.total).to.eq(1)
            expect(res.body.links.self).to.eq("http://localhost:8083/portal/environments/DEFAULT/apis")  
            //expect(res.data[0].name).to.eq(Test)
            //JSON.stringify(body)  
             })
        
    })  

    it('Get API not found', ()=>{                
        cy.request({
        method: 'GET',
            url: 'http://localhost:8083/portal/environments/DEFAULT/apis/:'+apiID,
            failOnStatusCode: false,
            headers: {
                'authorization': "Basic YWRtaW46YWRtaW4=",
                'Cookie': "XSRF-TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJncmF2aXRlZS1tYW5hZ2VtZW50LWF1dGgiLCJpYXQiOjE2Mzg0MjU2NDgsInRva2VuIjoiYzY0MjVkZmItNGM0Zi00MTYzLTlmOGItZTU5MTdiNWI3OTlkIn0.mA4pLn2E3t-aLOBWj6xHxfBByzwzJ7eO4nTfnyu1EqM",
                },    
        }).as ("testResponse")
        .then((res)=>{
            cy.log(res)

        expect (res.status).to.eq(404)
        expect (res.body.errors[0].status).to.eq("404")
        //expect (res.body.errors[0].message).to.eq("Api [70b81cf5-0210-46c1-b81c-f50210e6c108] can not be found.")
        expect (res.body.errors[0].code).to.eq("errors.api.notFound")
        //expect (res.body.errors[0].parameters.api).to.eq("70b81cf5-0210-46c1-b81c-f50210e6c108")   
       
        })
    })    
})
