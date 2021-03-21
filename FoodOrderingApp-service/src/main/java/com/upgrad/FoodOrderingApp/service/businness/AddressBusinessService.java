package com.upgrad.FoodOrderingApp.service.businness;

import com.upgrad.FoodOrderingApp.service.dao.AddressDao;
import com.upgrad.FoodOrderingApp.service.dao.CustomerDao;
import com.upgrad.FoodOrderingApp.service.entity.AddressEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAddressEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthTokenEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AddressNotFoundException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SaveAddressException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AddressBusinessService {

    @Autowired
    private AddressDao addressDao;

    @Autowired
    private CustomerDao customerDao;

    @Transactional
    public AddressEntity saveAddress(AddressEntity addressEntity, final String authorizationToken) throws AddressNotFoundException, SaveAddressException, AuthorizationFailedException {

        //Validate customer
        CustomerAuthTokenEntity customerAuthTokenEntity = customerDao.getCustomerAuthToken(authorizationToken);
        if (customerAuthTokenEntity==null) {
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in.");
        } else if (customerAuthTokenEntity.getLogoutAt() != null) {
            throw new AuthorizationFailedException("ATHR-002", "Customer is logged out. Log in again to access this endpoint.");
        } else if (customerAuthTokenEntity.getExpiresAt().compareTo(ZonedDateTime.now()) < 0) {
            throw new AuthorizationFailedException("ATHR-003", "Your session is expired. Log in again to access this endpoint.");
        }

        //Check requirements for request
        if(addressEntity.getState()==null) {
            throw new AddressNotFoundException("ANF-002","No state by this id");
        } else if (addressEntity.getFlatBuilNumber()==null || addressEntity.getLocality()==null || addressEntity.getCity()==null || addressEntity.getPincode()==null || addressEntity.getState()==null) {
            throw new SaveAddressException("SAR-001", "No field can be empty");
        } else if (!addressEntity.getPincode().matches("[0-9]+") ||
                    addressEntity.getPincode().length() != 6){
            throw new SaveAddressException("SAR-002", "Invalid pincode");
        }

        //Once customer and request are validated, save address in database
        AddressEntity savedAddressEntity = addressDao.createAddress(addressEntity);

        //Map the saved address entity to the corresponding customer in customer_address table of database
        CustomerEntity customer = customerAuthTokenEntity.getCustomer();
        CustomerAddressEntity customerAddressEntity = new CustomerAddressEntity();
        customerAddressEntity.setCustomer(customer);
        customerAddressEntity.setAddress(savedAddressEntity);
        addressDao.mapCustomerToAddress(customerAddressEntity);

        return savedAddressEntity;
    }


    @Transactional
    public List<AddressEntity> getAllAddressesByCustomer(final String authorizationToken) throws AuthorizationFailedException {

        //Validate customer
        CustomerAuthTokenEntity customerAuthTokenEntity = customerDao.getCustomerAuthToken(authorizationToken);
        if (customerAuthTokenEntity==null) {
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in.");
        } else if (customerAuthTokenEntity.getLogoutAt() != null) {
            throw new AuthorizationFailedException("ATHR-002", "Customer is logged out. Log in again to access this endpoint.");
        } else if (customerAuthTokenEntity.getExpiresAt().compareTo(ZonedDateTime.now()) < 0) {
            throw new AuthorizationFailedException("ATHR-003", "Your session is expired. Log in again to access this endpoint.");
        }

        CustomerEntity customer = customerAuthTokenEntity.getCustomer();
        List<AddressEntity> customerAddresses = addressDao.getAllAddressesByCustomer(customer);
        Collections.reverse(customerAddresses);
        return customerAddresses;
    }


}
