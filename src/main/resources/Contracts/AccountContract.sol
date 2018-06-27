pragma solidity ^0.4.0;

import './Pausable.sol';
import './StorageContract.sol';


contract AccountContract is Pausable, IStorageContractClient {

    StorageContract public storageContract;

    uint256 constant public storageIdentifier = uint256(keccak256("AccountContract"));
    uint256 constant public publicKeyField = uint256(keccak256("publicKeyField"));
    uint256 constant public nonceField = uint256(keccak256("nonceField"));

    function AccountContract(StorageContract _storageContract) public {
        storageContract = (_storageContract != address(0)) ? _storageContract : new StorageContract();
    }

    function registerPublicKey(uint256 publicKeyX, uint256 publicKeyY) public onlyOwner whenNotPaused {
        require(!isRegisteredPublicKey(publicKeyX));
        storageContract.set(uint256(keccak256(publicKeyField, publicKeyX)), publicKeyY);
    }

    function unregisterPublicKey(uint256 publicKeyX) public onlyOwner whenNotPaused {
        storageContract.erase(uint256(keccak256(publicKeyField, publicKeyX)));
    }

    function setNonceForPublicKeyX(uint256 publicKeyX, uint256 nonce) public onlyOwner whenNotPaused {
        storageContract.set(uint256(keccak256(nonceField, publicKeyX)), nonce);
    }

    function publicKeyYForX(uint256 publicKeyX) public constant returns(uint256) {
        return storageContract.get(uint256(keccak256(publicKeyField, publicKeyX)));
    }

    function isRegisteredPublicKey(uint256 publicKeyX) public constant returns(bool) {
        return publicKeyYForX(publicKeyX) != 0;
    }

    function nonceForPublicKeyX(uint256 publicKeyX) public constant returns(uint256) {
        return storageContract.get(uint256(keccak256(nonceField, publicKeyX)));
    }

}
