pragma solidity >=0.4.20;

import './Pausable.sol';


contract NameServiceContract is Pausable {

    string[] public names;
    mapping(bytes32 => uint) private indexOfNameHash; // Incremented indexes
    mapping(bytes32 => address) private addressOfNameHash;

    function incrementedIndexOfName(string memory name) public view returns(uint) {
        return indexOfNameHash[keccak256(abi.encodePacked(name))];
    }

    function addressOfName(string memory name) public view returns(address) {
        return addressOfNameHash[keccak256(abi.encodePacked(name))];
    }

    function setAddressOf(string memory name, address addr) public onlyOwner whenNotPaused {
        bytes32 nameHash = keccak256(abi.encodePacked(name));
        if (indexOfNameHash[nameHash] == 0) {
            names.push(name);
            indexOfNameHash[nameHash] = names.length; // Incremented
        }
        addressOfNameHash[nameHash] = addr;
    }

}
