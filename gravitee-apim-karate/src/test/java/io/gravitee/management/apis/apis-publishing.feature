Feature: API Publishing

  Background:
    * def adminAuth = call read('classpath:basic-auth.js') { username: 'admin', password: 'admin' }


  Scenario: get all users and then get the first user by id
    Given url managementUrl
    And path 'apis'
    And header Authorization = adminAuth
    When method get
    Then status 200
#    Then match response contains {id: 1a1690e2-9431-4978-9690-e294312978e9}

#    * print response
    * def api = get[0] response[?(@.id=='1a1690e2-9431-4978-9690-e294312978e9')]
    * match api.id == '1a1690e2-9431-4978-9690-e294312978e9'
#
    Given path 'apis', api.id
    And header Authorization = adminAuth
    When method get
    Then status 200
    Then print response

#  Scenario: create a user and then get it by id
#    * def user =
#      """
#      {
#        "name": "Test User",
#        "username": "testuser",
#        "email": "test@user.com",
#        "address": {
#          "street": "Has No Name",
#          "suite": "Apt. 123",
#          "city": "Electri",
#          "zipcode": "54321-6789"
#        }
#      }
#      """
#
#    Given url 'https://jsonplaceholder.typicode.com/users'
#    And request user
#    When method post
#    Then status 201
#
#    * def id = response.id
#    * print 'created id is: ', id
#
#    Given path id
    # When method get
    # Then status 200
    # And match response contains user
  