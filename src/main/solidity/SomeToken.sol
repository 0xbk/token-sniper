import "./IERC20.sol";

abstract contract SomeToken is IERC20  {
    function maxTransferAmount() public virtual view returns (uint256);
}
